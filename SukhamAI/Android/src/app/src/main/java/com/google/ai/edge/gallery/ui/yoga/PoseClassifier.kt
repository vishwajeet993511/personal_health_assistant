/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.ui.yoga

import android.util.Log
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Geometric pose matching over the MediaPipe landmarks.
 *
 * The 33 joint coordinates are already in memory and are exact; asking a 2B vision model to
 * re-derive the pose by looking at a *picture* of them throws that away, which is why naming
 * used to collapse onto "Warrior" for almost every input. Here we score the skeleton against
 * hand-authored joint-angle templates and hand the ranked candidates to the model, which then
 * only has to write the correction.
 *
 * **These templates are hand-authored and approximate.** They encode what each pose looks like
 * in joint angles, not measurements taken from real practitioners, so expect to tune
 * [POSE_TEMPLATES] against actual poses. The ranking is a strong prior for the model, not ground
 * truth — which is why the prompt lets it override a bad match.
 */
object PoseClassifier {

  private const val TAG = "PoseClassifier"

  /** Above this leg angle the body is lying down or inverted, not standing. */
  private const val MAX_STANDING_LEG_LEAN = 60f
  /** Below this wrist height the hands are bearing weight on the floor. */
  private const val MIN_STANDING_WRIST_HEIGHT = 0.25f
  /** Hands-down only rules a pose out when the legs are off vertical too (cf. a forward fold). */
  private const val HANDS_DOWN_LEG_LEAN = 25f

  /** A candidate pose and how well the skeleton matched it, 0..1 (1 = exact). */
  data class PoseMatch(val name: String, val score: Float)

  // MediaPipe BlazePose landmark indices.
  private const val L_SHOULDER = 11
  private const val R_SHOULDER = 12
  private const val L_ELBOW = 13
  private const val R_ELBOW = 14
  private const val L_WRIST = 15
  private const val R_WRIST = 16
  private const val L_HIP = 23
  private const val R_HIP = 24
  private const val L_KNEE = 25
  private const val R_KNEE = 26
  private const val L_ANKLE = 27
  private const val R_ANKLE = 28

  /**
   * The feature vector, in the order [POSE_TEMPLATES] lists its numbers.
   *
   * Everything is an angle in degrees except the last two, which are ratios scaled into a
   * comparable range so one weighted distance works across all of them.
   */
  private const val F_KNEE_L = 0
  private const val F_KNEE_R = 1
  private const val F_HIP_L = 2
  private const val F_HIP_R = 3
  private const val F_ELBOW_L = 4
  private const val F_ELBOW_R = 5
  private const val F_SHOULDER_L = 6
  private const val F_SHOULDER_R = 7
  private const val F_TORSO_LEAN = 8
  private const val F_STANCE = 9
  private const val F_FOOT_LIFT = 10
  private const val F_LEG_LEAN = 11
  private const val FEATURE_COUNT = 12

  /**
   * Weights for **identifying** the pose, as distinct from judging it.
   *
   * Templates encode well-executed poses, so scoring every feature equally means the worse
   * someone's form, the further they drift from their own pose's template — and the more likely
   * they are misnamed. That is backwards for a coaching app: bad form is exactly when naming
   * has to hold up.
   *
   * So identification leans on the features that survive bad execution — is the body prone or
   * upright, how wide is the stance, is a foot lifted, are the knees bent or straight. A sloppy
   * Cobra is still unmistakably "prone, legs flat, chest lifted". The features where form errors
   * actually live (hips, elbows, shoulders, torso lean) still carry a little signal, because
   * they do help separate genuinely different poses, but nowhere near enough to rename a pose
   * just because it is done badly. Those are scored separately in [formDeviations].
   */
  private val IDENTITY_WEIGHTS =
    floatArrayOf(
      1.2f, 1.2f, // knees — bent vs straight is structural
      0.6f, 0.6f, // hips — form-sensitive, still some signal
      0.2f, 0.2f, // elbows — mostly form
      0.2f, 0.2f, // shoulders — mostly form
      0.8f, // torso lean — partly structural, partly form
      1.3f, // stance width — structural
      1.3f, // foot lift — structural
      1.8f, // leg lean — the strongest structural cue of all
    )

  /** Human-readable names, used when reporting how far a pose sits from its reference. */
  private val FEATURE_LABELS =
    arrayOf(
      "left knee", "right knee", "left hip", "right hip", "left elbow", "right elbow",
      "left shoulder", "right shoulder", "torso lean", "stance width", "foot lift", "leg angle",
    )

  /**
   * Measured reference geometry, keyed by display name. See [POSE_TEMPLATES_78] — these are
   * extracted from reference photographs with this app's own landmarker, replacing the
   * hand-authored table whose guessed angles caused every misclassification we hit.
   */
  /**
   * The poses the coach actually classifies against.
   *
   * All 78 measured references stay in [POSE_TEMPLATES_78], but ranking against every one of
   * them was unreliable: the closest pairs sat 4.6 apart in feature space, so ordinary landmark
   * noise was enough to flip a standing person between neighbouring asanas from frame to frame.
   * Published results say the same thing — a DenseNet-201 on the 82-class Yoga-82 benchmark
   * reaches only ~79% top-1, so fine-grained separation at that scale is hard even for much
   * larger models than anything that fits on a phone.
   *
   * Restricting to ten well-separated, commonly practised poses lifts the worst-case separation
   * to 21.2 and the median to 34.1, which is comfortably outside the noise floor. Add to this
   * list only after checking the new pose does not sit on top of an existing one.
   */
  val CURATED_POSES =
    listOf(
      "Mountain (Tadasana)",
      "Upward Salute (Urdhva Hastasana)",
      "Chair (Utkatasana)",
      "Tree (Vrksasana)",
      "Warrior 1 (Virabhadrasana I)",
      "Warrior 2 (Virabhadrasana II)",
      "Warrior 3 (Virabhadrasana III)",
      "Triangle (Utthita Trikonasana)",
      "Standing Forward Fold (Uttanasana)",
      "Extended Side Angle (Utthita Parsvakonasana)",
    )

  private val POSE_TEMPLATES: Map<String, FloatArray> =
    POSE_TEMPLATES_78.filter { it.name in CURATED_POSES }.associate { it.name to it.features }

  /** Coarse family (standing, seated, back bend, ...) for each pose, used to group candidates. */
  private val POSE_CATEGORY: Map<String, String> =
    POSE_TEMPLATES_78.associate { it.name to it.category }

  /** The family a pose belongs to, or null if unknown. */
  fun categoryOf(poseName: String): String? = POSE_CATEGORY[poseName]

  /**
   * Returns the [topN] closest poses, best first. Empty when no person was detected.
   *
   * Each template is scored twice — as-is and with left/right swapped — and the better of the
   * two is kept, because every asana can be practised on either side and a mirrored Warrior II
   * is still Warrior II.
   */
  /**
   * Whether the body is upright enough to be one of the standing poses we classify.
   *
   * With only standing templates loaded, a person on the floor would otherwise be forced onto
   * the nearest standing label — Downward Dog came out as "Standing Forward Fold", Cat as
   * "Chair". Refusing to answer is the honest result, and it lets the UI ask them to stand
   * instead of coaching a pose they are not in.
   *
   * Two measurements, both validated against the 78 reference images:
   *  - legs horizontal (legLean > 60) means lying or inverted;
   *  - hands down near the floor while the legs are not vertical means a hands-and-knees or
   *    hands-and-feet pose. The second clause needs the leg condition because a standing
   *    forward fold also has the hands low, but its legs stay vertical.
   */
  fun isStandingPose(result: PoseLandmarkerResult): Boolean {
    val f = extractFeatures(result) ?: return false
    if (f[F_LEG_LEAN] > MAX_STANDING_LEG_LEAN) return false
    val w = wristHeightAboveAnkles(result) ?: return true
    return !(w < MIN_STANDING_WRIST_HEIGHT && f[F_LEG_LEAN] > HANDS_DOWN_LEG_LEAN)
  }

  /** Wrist height above the ankles, in leg-lengths. Negative means hands below the feet. */
  private fun wristHeightAboveAnkles(result: PoseLandmarkerResult): Float? {
    val pts = worldPoints(result) ?: return null
    val legLength =
      (dist(pts[L_HIP], pts[L_KNEE]) + dist(pts[L_KNEE], pts[L_ANKLE])).coerceAtLeast(1e-4f)
    val ankleY = (pts[L_ANKLE].y + pts[R_ANKLE].y) / 2f
    val wristY = (pts[L_WRIST].y + pts[R_WRIST].y) / 2f
    // y grows downward, so a positive result means the wrists sit above the ankles.
    return (ankleY - wristY) / legLength
  }

  /** The raw 12-feature vector, exposed so callers can average it across frames. */
  fun featuresOf(result: PoseLandmarkerResult): FloatArray? = extractFeatures(result)

  fun classify(result: PoseLandmarkerResult, topN: Int = 3): List<PoseMatch> {
    if (!isStandingPose(result)) {
      Log.d(TAG, "Not a standing pose; declining to classify.")
      return emptyList()
    }
    val features = extractFeatures(result) ?: return emptyList()
    return classifyFeatures(features, topN)
  }

  /**
   * Ranks a feature vector that may have been pooled over several frames.
   *
   * Classifying a single frame made the label jitter — the same held pose could be reported as
   * two different asanas on consecutive triggers, because one noisy landmark estimate was enough
   * to reorder near neighbours. Callers now hand in a per-feature median over the hold window
   * instead.
   */
  fun classifyFeatures(features: FloatArray, topN: Int = 3): List<PoseMatch> {
    Log.d(
      TAG,
      "FEATURES kneeL=%.0f kneeR=%.0f hipL=%.0f hipR=%.0f elbowL=%.0f elbowR=%.0f".format(
        features[F_KNEE_L], features[F_KNEE_R], features[F_HIP_L],
        features[F_HIP_R], features[F_ELBOW_L], features[F_ELBOW_R],
      ) +
        " shoulderL=%.0f shoulderR=%.0f torsoLean=%.0f stance=%.0f footLift=%.0f".format(
          features[F_SHOULDER_L], features[F_SHOULDER_R], features[F_TORSO_LEAN],
          features[F_STANCE], features[F_FOOT_LIFT],
        ) + " legLean=%.0f".format(features[F_LEG_LEAN]),
    )
    val mirrored = mirror(features)
    return POSE_TEMPLATES.map { (name, template) ->
        val d = min(distance(features, template), distance(mirrored, template))
        // Map distance to a 0..1 score. The 40f constant sets how quickly confidence decays;
        // a perfect match scores 1.0 and a ~40-degree average miss scores ~0.5.
        PoseMatch(name = name, score = 1f / (1f + d / 40f))
      }
      .sortedByDescending { it.score }
      .also { ranked ->
        Log.d(TAG, "RANKING " + ranked.take(6).joinToString { "%s=%.2f".format(it.name, it.score) })
      }
      .take(topN)
  }

  /**
   * The skeleton described in plain English, for the model to reason with.
   *
   * A 2B model is weak at reading angles off a rendered image but perfectly capable of using
   * them when stated as facts. This turns the landmark numbers — which are exact — into the
   * evidence it needs to confirm or reject the ranking, instead of asking it to eyeball
   * geometry and hoping.
   */
  fun describeGeometry(result: PoseLandmarkerResult): List<String> {
    val f = extractFeatures(result) ?: return emptyList()
    val out = mutableListOf<String>()

    out +=
      when {
        f[F_LEG_LEAN] > 70f -> "the legs are horizontal, lying flat along the floor"
        f[F_LEG_LEAN] > 40f -> "the legs are angled roughly halfway between upright and flat"
        f[F_LEG_LEAN] > 18f -> "the legs are angled out from vertical, as in a wide stance"
        else -> "the legs are close to vertical, as in standing"
      }

    out +=
      when {
        f[F_STANCE] > 70f -> "the feet are very wide apart"
        f[F_STANCE] > 35f -> "the feet are apart in a lunge or wide stance"
        else -> "the feet are close together"
      }

    if (f[F_FOOT_LIFT] > 35f) out += "one foot is lifted well clear of the other"

    fun knee(label: String, v: Float) =
      when {
        v > 160f -> "the $label leg is straight"
        v > 120f -> "the $label knee is slightly bent (about ${v.toInt()} degrees)"
        else -> "the $label knee is deeply bent (about ${v.toInt()} degrees)"
      }
    out += knee("left", f[F_KNEE_L])
    out += knee("right", f[F_KNEE_R])

    // Hips are signed: above 180 means the thigh has swung behind the body (a backbend).
    val hip = (f[F_HIP_L] + f[F_HIP_R]) / 2f
    out +=
      when {
        hip > 188f -> "the hips are extended ${(hip - 180f).toInt()} degrees past straight, " +
          "so the back is arching backwards"
        hip > 165f -> "the body is roughly straight from shoulders through hips to knees"
        hip > 110f -> "the hips are folded about ${(180f - hip).toInt()} degrees"
        else -> "the hips are deeply folded, about ${(180f - hip).toInt()} degrees"
      }

    val elbow = (f[F_ELBOW_L] + f[F_ELBOW_R]) / 2f
    out +=
      if (elbow > 155f) "the arms are straight at the elbows"
      else "the elbows are bent to about ${elbow.toInt()} degrees"

    val shoulder = (f[F_SHOULDER_L] + f[F_SHOULDER_R]) / 2f
    out +=
      when {
        shoulder > 140f -> "the arms are raised up overhead"
        shoulder > 70f -> "the arms are out away from the body, roughly shoulder height"
        else -> "the arms are down close to the body"
      }

    out +=
      when {
        f[F_TORSO_LEAN] < 15f -> "the torso is upright"
        f[F_TORSO_LEAN] < 55f -> "the torso is leaning ${f[F_TORSO_LEAN].toInt()} degrees " +
          "from vertical"
        else -> "the torso is close to horizontal (${f[F_TORSO_LEAN].toInt()} degrees from " +
          "vertical)"
      }
    return out
  }

  /**
   * Where this skeleton departs most from the reference for [poseName], largest first.
   *
   * This is the half of the job the identity ranking deliberately ignores. Because the template
   * describes a well-executed pose, the per-feature gap against it IS the form error — measured,
   * not guessed — so the model can be pointed at the joint that is actually off rather than
   * inventing something plausible.
   */
  fun formDeviations(result: PoseLandmarkerResult, poseName: String, limit: Int = 2): List<String> {
    val f = extractFeatures(result) ?: return emptyList()
    val template = POSE_TEMPLATES[poseName] ?: return emptyList()
    val mirroredF = mirror(f)
    // Score against whichever side matches better, so a left-side pose is not reported as
    // being wrong on every limb.
    val use = if (distance(mirroredF, template) < distance(f, template)) mirroredF else f
    return (0 until FEATURE_COUNT)
      .map { i -> Triple(i, use[i] - template[i], abs(use[i] - template[i])) }
      // Ratio features are noisier and less interpretable as a "fix", so ignore them here.
      .filter { (i, _, mag) -> i != F_STANCE && i != F_FOOT_LIFT && mag >= 22f }
      .sortedByDescending { it.third }
      .take(limit)
      .map { (i, signed, mag) ->
        val direction = if (signed > 0) "more open than" else "more closed than"
        "${FEATURE_LABELS[i]} is ${mag.toInt()} degrees $direction the reference"
      }
  }

  /** Swaps every left/right feature pair so a template can be matched against either side. */
  private fun mirror(f: FloatArray): FloatArray {
    val m = f.copyOf()
    fun swap(a: Int, b: Int) {
      val t = m[a]
      m[a] = m[b]
      m[b] = t
    }
    swap(F_KNEE_L, F_KNEE_R)
    swap(F_HIP_L, F_HIP_R)
    swap(F_ELBOW_L, F_ELBOW_R)
    swap(F_SHOULDER_L, F_SHOULDER_R)
    return m
  }

  /** Weighted RMS difference across the feature vector. */
  private fun distance(a: FloatArray, b: FloatArray): Float {
    var sum = 0f
    var weight = 0f
    for (i in 0 until FEATURE_COUNT) {
      val d = a[i] - b[i]
      sum += IDENTITY_WEIGHTS[i] * d * d
      weight += IDENTITY_WEIGHTS[i]
    }
    return sqrt(sum / weight)
  }

  /**
   * World landmarks as points. These are metric and hip-centred, so angles computed from them
   * are free of the image aspect-ratio and camera-distance distortion that plagues the
   * normalised ones; we fall back to normalised only when world landmarks are unavailable.
   */
  private fun worldPoints(result: PoseLandmarkerResult): List<Pt>? {
    val world = result.worldLandmarks().firstOrNull()
    if (world != null && world.size > R_ANKLE) {
      return world.map { lm: Landmark -> Pt(lm.x(), lm.y(), lm.z()) }
    }
    val norm = result.landmarks().firstOrNull() ?: return null
    if (norm.size <= R_ANKLE) return null
    return norm.map { lm: NormalizedLandmark -> Pt(lm.x(), lm.y(), lm.z()) }
  }

  private fun extractFeatures(result: PoseLandmarkerResult): FloatArray? {
    val pts = worldPoints(result) ?: return null

    val f = FloatArray(FEATURE_COUNT)
    f[F_KNEE_L] = angle(pts[L_HIP], pts[L_KNEE], pts[L_ANKLE])
    f[F_KNEE_R] = angle(pts[R_HIP], pts[R_KNEE], pts[R_ANKLE])

    // Hips are measured SIGNED. An unsigned acos saturates at 180, so folding forward 30 degrees
    // and arching back 30 degrees both read as 150 — which makes every backbend (Cobra, Camel,
    // Bridge) indistinguishable from a forward fold on its most characteristic feature. We
    // resolve the direction against the torso's own "front" normal, so extension reports above
    // 180 and flexion below it.
    val front = frontNormal(pts)
    f[F_HIP_L] = signedHipAngle(pts[L_SHOULDER], pts[L_HIP], pts[L_KNEE], front)
    f[F_HIP_R] = signedHipAngle(pts[R_SHOULDER], pts[R_HIP], pts[R_KNEE], front)
    f[F_ELBOW_L] = angle(pts[L_SHOULDER], pts[L_ELBOW], pts[L_WRIST])
    f[F_ELBOW_R] = angle(pts[R_SHOULDER], pts[R_ELBOW], pts[R_WRIST])
    f[F_SHOULDER_L] = angle(pts[L_ELBOW], pts[L_SHOULDER], pts[L_HIP])
    f[F_SHOULDER_R] = angle(pts[R_ELBOW], pts[R_SHOULDER], pts[R_HIP])

    val hipMid = mid(pts[L_HIP], pts[R_HIP])
    val shoulderMid = mid(pts[L_SHOULDER], pts[R_SHOULDER])
    // Degrees off vertical: 0 upright, 90 horizontal. y grows downward in both coordinate
    // spaces, so the spine vector points in -y when the person is upright.
    val spineX = shoulderMid.x - hipMid.x
    val spineY = shoulderMid.y - hipMid.y
    val spineZ = shoulderMid.z - hipMid.z
    val horizontal = hypot(spineX, spineZ)
    f[F_TORSO_LEAN] = Math.toDegrees(atan2(horizontal, -spineY).toDouble()).toFloat()

    val shoulderWidth = dist(pts[L_SHOULDER], pts[R_SHOULDER]).coerceAtLeast(1e-4f)
    val ankleGap = dist(pts[L_ANKLE], pts[R_ANKLE])
    f[F_STANCE] = (ankleGap / shoulderWidth) * 50f

    val legLength =
      (dist(pts[L_HIP], pts[L_KNEE]) + dist(pts[L_KNEE], pts[L_ANKLE])).coerceAtLeast(1e-4f)
    f[F_FOOT_LIFT] = (abs(pts[L_ANKLE].y - pts[R_ANKLE].y) / legLength) * 100f

    // How far the legs sit off vertical: ~0 standing, ~90 lying flat on the floor.
    //
    // Without this the classifier could tell a straight leg from a bent one but had no idea
    // whether the person was upright or prone — so Cobra (legs flat on the mat) and Pyramid
    // (legs essentially vertical) occupied nearly the same point, differing only in features
    // that a backbend and a forward fold happen to share. It is the most obvious difference to
    // anyone looking at the two poses, hence the highest weight in the vector.
    f[F_LEG_LEAN] = (legLean(pts[L_HIP], pts[L_ANKLE]) + legLean(pts[R_HIP], pts[R_ANKLE])) / 2f

    return f
  }

  /**
   * Unit vector pointing out of the chest, from the shoulder line crossed with the spine.
   *
   * Gives the body its own frame of reference, so "knee in front of the hip" versus "knee behind
   * it" is decidable regardless of which way the person faces the camera.
   */
  private fun frontNormal(pts: List<Pt>): Pt {
    val lateralX = pts[R_SHOULDER].x - pts[L_SHOULDER].x
    val lateralY = pts[R_SHOULDER].y - pts[L_SHOULDER].y
    val lateralZ = pts[R_SHOULDER].z - pts[L_SHOULDER].z
    val hipMid = mid(pts[L_HIP], pts[R_HIP])
    val shoulderMid = mid(pts[L_SHOULDER], pts[R_SHOULDER])
    val upX = shoulderMid.x - hipMid.x
    val upY = shoulderMid.y - hipMid.y
    val upZ = shoulderMid.z - hipMid.z
    // cross(lateral, up)
    val cx = lateralY * upZ - lateralZ * upY
    val cy = lateralZ * upX - lateralX * upZ
    val cz = lateralX * upY - lateralY * upX
    val n = sqrt(cx * cx + cy * cy + cz * cz)
    return if (n < 1e-6f) Pt(0f, 0f, 1f) else Pt(cx / n, cy / n, cz / n)
  }

  /**
   * Hip angle where flexion reads below 180 and extension above it.
   *
   * The magnitude is the ordinary interior angle; the sign comes from which side of the chest
   * plane the thigh falls on.
   */
  private fun signedHipAngle(shoulder: Pt, hip: Pt, knee: Pt, front: Pt): Float {
    val raw = angle(shoulder, hip, knee)
    val deviation = 180f - raw
    val tx = knee.x - hip.x
    val ty = knee.y - hip.y
    val tz = knee.z - hip.z
    val facing = tx * front.x + ty * front.y + tz * front.z
    // Sign verified empirically against the 78 reference images: with this orientation every
    // backbend (Cobra 222, Camel 242, Wheel 254) lands above 180 and every forward fold
    // (Down Dog 71, Child's 34, Uttanasana 29) below it. The opposite convention, which is what
    // this originally used, had them exactly backwards.
    return if (facing >= 0f) 180f + deviation else 180f - deviation
  }

  /** Degrees the hip-to-ankle axis sits off vertical. 0 = straight down, 90 = horizontal. */
  private fun legLean(hip: Pt, ankle: Pt): Float {
    val dx = ankle.x - hip.x
    val dy = ankle.y - hip.y
    val dz = ankle.z - hip.z
    val horizontal = hypot(dx, dz)
    // y grows downward, so a leg hanging straight down has a positive dy.
    return Math.toDegrees(atan2(horizontal, dy).toDouble()).toFloat()
  }

  private data class Pt(val x: Float, val y: Float, val z: Float)

  private fun mid(a: Pt, b: Pt) = Pt((a.x + b.x) / 2f, (a.y + b.y) / 2f, (a.z + b.z) / 2f)

  private fun dist(a: Pt, b: Pt): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    return sqrt(dx * dx + dy * dy + dz * dz)
  }

  /** Interior angle at [b] in the chain a-b-c, in degrees (180 = straight limb). */
  private fun angle(a: Pt, b: Pt, c: Pt): Float {
    val v1x = a.x - b.x
    val v1y = a.y - b.y
    val v1z = a.z - b.z
    val v2x = c.x - b.x
    val v2y = c.y - b.y
    val v2z = c.z - b.z
    val n1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
    val n2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
    if (n1 < 1e-6f || n2 < 1e-6f) return 180f
    val cos = ((v1x * v2x + v1y * v2y + v1z * v2z) / (n1 * n2)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(cos).toDouble()).toFloat()
  }
}
