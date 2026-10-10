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

/**
 * The faults a yoga teacher actually looks for, per pose.
 *
 * Asking a 2B model to invent a correction from an image produces vague, interchangeable advice
 * ("engage your core") because it has nothing specific to look *for*. Now that [PoseClassifier]
 * names the pose from measured joint angles rather than guessing, we can hand the model that
 * pose's known faults and ask which one applies — turning open-ended generation into a
 * checklist, which small models are far better at.
 *
 * Each entry is ordered most-common first, phrased as the *correction* rather than the error so
 * the model can quote it almost verbatim and still be giving an instruction.
 */
object PoseCues {

  private val CUES: Map<String, List<String>> =
    mapOf(
      "Cobra (Bhujangasana)" to
        listOf(
          "soften the shoulders down away from the ears",
          "hug the elbows in against the ribs instead of splaying them wide",
          "keep the hips down on the floor and lift with the back, not the arms",
          "bring the hands back under the shoulders",
        ),
      "Warrior 1 (Virabhadrasana I)" to
        listOf(
          "bend the front knee to 90 degrees over the ankle",
          "straighten and press back through the back leg",
          "square the hips forward",
          "drop the shoulders away from the ears",
        ),
      "Warrior 2 (Virabhadrasana II)" to
        listOf(
          "bend the front knee until it stacks over the ankle",
          "lift the arms level with the shoulders",
          "stack the torso upright over the hips instead of leaning forward",
          "turn the back foot parallel to the back edge of the mat",
        ),
      "Warrior 3 (Virabhadrasana III)" to
        listOf(
          "level the lifted hip down toward the floor",
          "bring the torso parallel to the floor",
          "straighten the standing leg without locking the knee",
          "reach the lifted leg back in line with the spine",
        ),
      "Triangle (Utthita Trikonasana)" to
        listOf(
          "straighten the front leg",
          "reach the torso longer instead of collapsing into the bottom hand",
          "stack the top shoulder over the bottom one",
          "lift the bottom hand higher up the shin if the spine is rounding",
        ),
      "Extended Side Angle (Utthita Parsvakonasana)" to
        listOf(
          "bend the front knee to 90 degrees over the ankle",
          "reach the top arm forward over the ear rather than straight up",
          "lengthen the bottom side instead of leaning weight into the hand",
        ),
      "Tree (Vrksasana)" to
        listOf(
          "move the foot off the side of the knee, onto the calf or inner thigh",
          "level the hips instead of pushing the standing hip out",
          "press the foot and leg into each other to stop the ankle wobbling",
        ),
      "Chair (Utkatasana)" to
        listOf(
          "sink the hips lower, as if sitting back into a chair",
          "shift the weight into the heels so the knees stop passing the toes",
          "lift the arms alongside the ears",
          "lift the chest instead of rounding the upper back",
        ),
      "Downward Dog (Adho Mukha Svanasana)" to
        listOf(
          "lift the hips higher and back",
          "bend the knees to lengthen the spine if the back is rounding",
          "turn the inner elbows forward and widen across the shoulder blades",
          "walk the hands further forward if the shoulders feel crowded",
        ),
      "Plank (Kumbhakasana)" to
        listOf(
          "lift the hips in line with the shoulders and heels",
          "stop the hips piking up toward the ceiling",
          "press the floor away so the upper back stops sinking between the shoulders",
        ),
      "Side Plank (Vasisthasana)" to
        listOf(
          "lift the bottom hip up in line with the shoulders and feet",
          "stack the top shoulder directly over the bottom hand",
        ),
      "Boat (Navasana)" to
        listOf(
          "lift the chest and lengthen the spine instead of rounding the lower back",
          "draw the shoulders down away from the ears",
        ),
      "Camel (Ustrasana)" to
        listOf(
          "press the hips forward over the knees",
          "lift the chest up before leaning back",
          "keep the neck long rather than letting the head drop straight back",
        ),
      "Child's Pose (Balasana)" to
        listOf(
          "walk the hands further forward to lengthen the spine",
          "soften the shoulders away from the ears",
        ),
      "Standing Forward Fold (Uttanasana)" to
        listOf(
          "bend the knees to let the spine lengthen",
          "shift the weight forward into the balls of the feet",
        ),
      "Bridge (Setu Bandha Sarvangasana)" to
        listOf(
          "press the feet down and lift the hips higher",
          "bring the feet closer to the hips, hip-width apart",
        ),
      "Upward Salute (Urdhva Hastasana)" to
        listOf(
          "drop the shoulders down away from the ears",
          "stack the ribs over the hips instead of arching the lower back",
        ),
      "Mountain (Tadasana)" to
        listOf(
          "stack the ears over the shoulders and the shoulders over the hips",
          "spread the weight evenly across both feet",
        ),
      "Half Moon (Ardha Chandrasana)" to
        listOf(
          "stack the top hip directly over the bottom hip",
          "lift the raised leg to hip height, parallel with the floor",
        ),
      "Low Lunge (Ashwa Sanchalanasana)" to
        listOf(
          "bring the front knee over the ankle, not past the toes",
          "draw the hips down and forward",
        ),
      "High Lunge (Utthita Ashwa Sanchalanasana)" to
        listOf(
          "bend the front knee to 90 degrees over the ankle",
          "press the back heel toward the wall behind you",
        ),
      "Pyramid (Parsvottanasana)" to
        listOf(
          "square the hips toward the front of the mat",
          "soften the front knee slightly instead of locking it",
        ),
      "Eagle (Garudasana)" to
        listOf("sink the hips lower", "lift the elbows to shoulder height"),
      "Dancer (Natarajasana)" to
        listOf(
          "kick the lifted foot back into the hand to open the chest",
          "keep the standing leg straight and the hips level",
        ),
      "Half Spinal Twist (Ardha Matsyendrasana)" to
        listOf(
          "sit up tall through the spine before twisting further",
          "turn from the mid-back rather than cranking with the arm",
        ),
      "Wide Angle Seated Forward Bend (Upavistha Konasana)" to
        listOf(
          "walk the hands back in line with the feet",
          "shift the weight forward into the balls of the feet",
        ),
      "Prayer Hands (Anjali Mudra)" to
        listOf(
          "drop the shoulders down away from the ears",
          "press the palms evenly together at the centre of the chest",
        ),
    )

  /**
   * One-line visual descriptions, written as what a camera sees rather than how the pose feels.
   *
   * The classifier ranks candidates from joint angles alone, and its templates are
   * hand-authored, so it will sometimes rank a confusable neighbour first — Triangle and
   * Extended Side Angle differ mainly in whether the front knee is bent. Giving the model a
   * description of each candidate turns "override if the image clearly contradicts it" from an
   * instruction it cannot act on into an actual check it can perform.
   */
  private val DESCRIPTIONS: Map<String, String> =
    mapOf(
      // --- Pairs the joint angles cannot separate: these descriptions are the only thing
      // --- letting the model break the tie, so each one names the single visual difference.
      "Upward Dog (Urdhva Mukha Svanasana)" to
        "chest lifted with arms STRAIGHT and the THIGHS LIFTED CLEAR of the floor, only hands " +
        "and tops of the feet touching down",
      "Table (Bharmanasana)" to
        "on hands and knees with a FLAT, level back, wrists under shoulders and knees under hips",
      "Cat (Marjariasana)" to
        "on hands and knees with the spine ROUNDED UP toward the ceiling and the head dropping",
      "Cow (Bitilasana)" to
        "on hands and knees with the belly DROPPING DOWN, chest and tailbone lifting",
      "Warrior 1 (Virabhadrasana I)" to
        "long stance, front knee bent, BACK FOOT FLAT and turned in, hips facing the front, " +
        "arms overhead",
      "High Lunge (Utthita Ashwa Sanchalanasana)" to
        "long stance, front knee bent, BACK HEEL LIFTED with the back leg straight, arms overhead",
      "Low Lunge (Ashwa Sanchalanasana)" to
        "front knee bent with the BACK KNEE RESTING ON THE FLOOR",
      "Lotus (Padmasana)" to
        "seated cross-legged with BOTH FEET resting up on the opposite thighs",
      "Easy Pose (Sukhasana)" to
        "seated in a simple cross-legged position with the feet tucked UNDER the opposite shins",
      "Headstand (Sirsasana)" to "inverted, balancing on the head and forearms, legs straight up",
      "Supported Headstand (Salamba Sirsasana)" to
        "inverted on the head with the FOREARMS DOWN and hands clasped, legs straight up",
      "Four-Limbed Staff (Chaturanga Dandasana)" to
        "a low plank with the ELBOWS BENT to about 90 degrees, hugged against the ribs",
      "Fish (Matsyasana)" to
        "lying on the back with the chest arched up and the crown of the head toward the floor",
      "Chair (Utkatasana)" to
        "both knees bent as if sitting back, torso leaning forward, arms reaching overhead",
      "Garland / Squat (Malasana)" to "a deep squat with the hips low and the heels down",
      "Corpse (Shavasana)" to "lying flat on the back, arms and legs relaxed and straight",
      "Mountain (Tadasana)" to "standing upright, feet together, arms down by the sides",
      "Upward Salute (Urdhva Hastasana)" to
        "standing upright, both arms stretched straight overhead",
      "Prayer Hands (Anjali Mudra)" to
        "standing upright, palms pressed together at the centre of the chest",
      "Chair (Utkatasana)" to
        "both knees bent as if sitting back, torso leaning forward, arms reaching overhead",
      "Tree (Vrksasana)" to
        "standing on one straight leg, the other foot tucked against the inner calf or thigh " +
        "with the knee out to the side",
      "Warrior 1 (Virabhadrasana I)" to
        "long stance, front knee bent, hips and chest facing the front, both arms overhead",
      "Warrior 2 (Virabhadrasana II)" to
        "wide stance, front knee bent to a right angle, torso upright and facing sideways, " +
        "both arms stretched out horizontally at shoulder height",
      "Warrior 3 (Virabhadrasana III)" to
        "balancing on one straight leg, torso and the other leg both horizontal, arms reaching " +
        "forward",
      "Triangle (Utthita Trikonasana)" to
        "wide stance with BOTH legs straight, torso tilted sideways, one hand low near the " +
        "shin or floor and the other arm pointing straight up",
      "Extended Side Angle (Utthita Parsvakonasana)" to
        "wide stance with the front knee BENT, torso tilted sideways over that thigh, top arm " +
        "reaching over the ear",
      "Half Moon (Ardha Chandrasana)" to
        "balancing on one straight leg with one hand on the floor, the other leg lifted to hip " +
        "height and the top arm pointing up",
      "Pyramid (Parsvottanasana)" to
        "both legs straight in a split stance, hips square, torso folded down over the front leg",
      "Dancer (Natarajasana)" to
        "balancing on one leg, the other foot held behind in the hand, chest lifted and the " +
        "opposite arm reaching forward",
      "Standing Forward Fold (Uttanasana)" to
        "standing with feet together, folded forward at the hips, head hanging down",
      "Wide Angle Seated Forward Bend (Upavistha Konasana)" to
        "feet very wide apart, legs straight, torso folded down with hands on the floor",
      "High Lunge (Utthita Ashwa Sanchalanasana)" to
        "long stance, front knee bent, back leg straight with the heel lifted, arms overhead",
      "Low Lunge (Ashwa Sanchalanasana)" to
        "front knee bent with the BACK KNEE DOWN on the floor, arms overhead",
      "Eagle (Garudasana)" to
        "standing with knees bent, one leg wrapped around the other and the arms twisted " +
        "together in front of the chest",
      "Boat (Navasana)" to
        "seated, balancing on the sitting bones with legs lifted and arms reaching forward",
      "Side Plank (Vasisthasana)" to
        "body in a straight diagonal line supported on one hand and the side of one foot, " +
        "other arm reaching up",
      "Downward Dog (Adho Mukha Svanasana)" to
        "an inverted V, hands and feet on the floor with the hips lifted high",
      "Plank (Kumbhakasana)" to
        "face down, body in one straight line from shoulders to heels, supported on the hands",
      "Cobra (Bhujangasana)" to
        "lying face down with the hips ON THE FLOOR, chest lifted and hands under the shoulders",
      "Camel (Ustrasana)" to
        "kneeling upright, back arched, hands reaching back to the heels",
      "Child's Pose (Balasana)" to
        "kneeling and folded forward, torso resting down over the thighs",
      "Half Spinal Twist (Ardha Matsyendrasana)" to
        "seated with one knee bent up across the other leg, torso twisted toward it",
      "Bridge (Setu Bandha Sarvangasana)" to
        "lying on the back with knees bent, feet flat and hips lifted off the floor",
    )

  /** Visual description of [poseName], or null when we have no note for it. */
  fun descriptionFor(poseName: String): String? = DESCRIPTIONS[poseName]

  /** Known faults for [poseName], most common first. Empty when we have no notes for it. */
  fun cuesFor(poseName: String): List<String> = CUES[poseName].orEmpty()
}
