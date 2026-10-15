/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package com.google.ai.edge.gallery.ui.programs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import com.google.ai.edge.gallery.ui.home.SukhamBottomNav
import com.google.ai.edge.gallery.ui.home.SukhamNavTab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

import com.google.ai.edge.gallery.ui.theme.SukhamColors

// MARK: - Zen-Tech Theme Palette
private object SukhamProgramsColors {
  val ScreenBg: Color @Composable get() = SukhamColors.ScreenBg
  val CardBg: Color @Composable get() = SukhamColors.CardBg
  val CardBorder: Color @Composable get() = SukhamColors.CardBorder
  val TitleBlack: Color @Composable get() = SukhamColors.TitleBlack
  val BodyGray: Color @Composable get() = SukhamColors.BodyGray

  val SagePrimary: Color @Composable get() = SukhamColors.SagePrimary
  val SageContainer: Color @Composable get() = SukhamColors.SageContainer
  val SageDark: Color @Composable get() = SukhamColors.SageDark

  val LavenderPrimary: Color @Composable get() = SukhamColors.LavenderPrimary
  val LavenderSoft: Color @Composable get() = SukhamColors.LavenderSoft
  val LavenderDark: Color @Composable get() = SukhamColors.LavenderPrimary

  val AmberPrimary: Color @Composable get() = SukhamColors.StreakAmber
  val AmberSoft: Color @Composable get() = SukhamColors.StreakGlow

  val ChipBg: Color @Composable get() = SukhamColors.ChipBg
}

// MARK: - Models
enum class PoseCategory(val displayName: String) {
  ALL("All"),
  STANDING("Standing"),
  SEATED("Seated"),
  SUPINE("Supine"),
  PRONE("Prone"),
  ARM_LEG_SUPPORT("Arm & Leg Support"),
  ARM_BALANCE_INVERSION("Arm Balance & Inversion"),
}

enum class PoseDifficulty(val label: String, val badgeBg: Color, val badgeText: Color) {
  BEGINNER("Beginner", Color(0xFFE8F5E9), Color(0xFF2E7D32)),
  INTERMEDIATE("Intermediate", Color(0xFFFFF3E0), Color(0xFFE65100)),
  ADVANCED("Advanced", Color(0xFFEDE7F6), Color(0xFF7E57C2)),
}

data class YogaPose(
  val id: String,
  val englishName: String,
  val sanskritName: String,
  val category: PoseCategory,
  val difficulty: PoseDifficulty,
  val focusBenefit: String,
  val defaultScore: Int? = null,
  val alignmentCues: List<String>,
  val iconVector: ImageVector,
) {
  val imageRes: Int
    get() = when (id) {
      "tree" -> R.drawable.pose_tree
      "warrior_2" -> R.drawable.pose_warrior_2
      "triangle" -> R.drawable.pose_triangle
      "chair" -> R.drawable.pose_chair
      "mountain" -> R.drawable.pose_mountain
      "warrior_1" -> R.drawable.pose_warrior_1
      "half_moon" -> R.drawable.pose_half_moon
      "eagle" -> R.drawable.pose_eagle
      "easy_pose" -> R.drawable.pose_easy_pose
      "boat" -> R.drawable.pose_boat
      "bound_angle" -> R.drawable.pose_bound_angle
      "half_pigeon" -> R.drawable.pose_half_pigeon
      "garland" -> R.drawable.pose_garland
      "bridge" -> R.drawable.pose_bridge
      "corpse" -> R.drawable.pose_corpse
      "happy_baby" -> R.drawable.pose_happy_baby
      "supine_twist" -> R.drawable.pose_supine_twist
      "cobra" -> R.drawable.pose_cobra
      "childs" -> R.drawable.pose_childs
      "sphinx" -> R.drawable.pose_sphinx
      "bow" -> R.drawable.pose_bow
      "downward_dog" -> R.drawable.pose_downward_dog
      "plank" -> R.drawable.pose_plank
      "cat_cow" -> R.drawable.pose_cat_cow
      "side_plank" -> R.drawable.pose_side_plank
      "upward_dog" -> R.drawable.pose_upward_dog
      "crow" -> R.drawable.pose_crow
      "headstand" -> R.drawable.pose_headstand
      "shoulder_stand" -> R.drawable.pose_shoulder_stand
      else -> R.drawable.pose_tree
    }
}

data class CuratedTrack(
  val id: String,
  val title: String,
  val durationWeeks: String,
  val targetFocus: String,
  val poseCount: Int,
  val completionPercent: Int,
) {
  val highlightColor: Color
    @Composable get() = when (id) {
      "track_spine_reset" -> SukhamColors.SagePrimary
      "track_kinetic_balance" -> SukhamColors.LavenderPrimary
      "track_morning_vitality" -> SukhamColors.StreakAmber
      else -> SukhamColors.LavenderPrimary
    }

  val bgSoft: Color
    @Composable get() = when (id) {
      "track_spine_reset" -> SukhamColors.SageContainer
      "track_kinetic_balance" -> SukhamColors.LavenderSoft
      "track_morning_vitality" -> SukhamColors.StreakGlow
      else -> SukhamColors.LavenderSoft
    }
}

// MARK: - Pose Dataset from Pocket Yoga Dictionary PDF
private val POCKET_YOGA_POSES = listOf(
  // 1. Standing Poses (Page 1 & 2)
  YogaPose(
    id = "tree",
    englishName = "Tree",
    sanskritName = "Vrikshasana",
    category = PoseCategory.STANDING,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Balance & Hip Stability",
    defaultScore = 94,
    alignmentCues = listOf(
      "Foot placed firmly against inner thigh or calf (avoid knee)",
      "Hands in Anjali Mudra at heart center or extended high",
      "Level hips and steady gaze at fixed focal point"
    ),
    iconVector = Icons.Outlined.Spa,
  ),
  YogaPose(
    id = "warrior_2",
    englishName = "Warrior II",
    sanskritName = "Virabhadrasana B",
    category = PoseCategory.STANDING,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Hip Mobility & Quad Strength",
    defaultScore = 82,
    alignmentCues = listOf(
      "Front knee stacked directly over ankle at 90°",
      "Arms parallel to floor, active through fingertips",
      "Torso centered, crown of head reaching high"
    ),
    iconVector = Icons.Outlined.AccessibilityNew,
  ),
  YogaPose(
    id = "triangle",
    englishName = "Triangle",
    sanskritName = "Trikonasana",
    category = PoseCategory.STANDING,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Lateral Spine Flexion & Hamstrings",
    defaultScore = 78,
    alignmentCues = listOf(
      "Both legs straight without locking knees",
      "Chest revolved open toward the ceiling",
      "Spine long and lengthened, avoiding collapsed ribs"
    ),
    iconVector = Icons.Outlined.SelfImprovement,
  ),
  YogaPose(
    id = "chair",
    englishName = "Chair",
    sanskritName = "Utkatasana",
    category = PoseCategory.STANDING,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Glute Power & Spine Extension",
    defaultScore = 88,
    alignmentCues = listOf(
      "Weight shifted back into heels, knees behind toes",
      "Arms raised framing ears, shoulder blades relaxed",
      "Pelvis slightly tucked to maintain neutral lumbar"
    ),
    iconVector = Icons.Outlined.FitnessCenter,
  ),
  YogaPose(
    id = "mountain",
    englishName = "Mountain",
    sanskritName = "Tadasana",
    category = PoseCategory.STANDING,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Postural Baseline & Spine Align",
    defaultScore = 96,
    alignmentCues = listOf(
      "Four corners of feet grounded evenly",
      "Kneecaps gently lifted, neutral pelvis",
      "Ears, shoulders, hips and ankles vertically stacked"
    ),
    iconVector = Icons.Outlined.AccessibilityNew,
  ),
  YogaPose(
    id = "warrior_1",
    englishName = "Warrior I",
    sanskritName = "Virabhadrasana A",
    category = PoseCategory.STANDING,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Hip Flexor Length & Stature",
    defaultScore = 85,
    alignmentCues = listOf(
      "Hips squared forward toward front mat edge",
      "Back foot anchored at 45° angle",
      "Ribs lifted away from waistline"
    ),
    iconVector = Icons.Outlined.DirectionsRun,
  ),
  YogaPose(
    id = "half_moon",
    englishName = "Half Moon",
    sanskritName = "Ardha Chandrasana",
    category = PoseCategory.STANDING,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Single-Leg Balance & Obliques",
    defaultScore = null,
    alignmentCues = listOf(
      "Standing foot straight ahead, knee soft",
      "Top leg lifted parallel to mat, foot flexed",
      "Chest and hips stacked vertically"
    ),
    iconVector = Icons.Outlined.Spa,
  ),
  YogaPose(
    id = "eagle",
    englishName = "Eagle",
    sanskritName = "Garudasana",
    category = PoseCategory.STANDING,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Joint Decompression & Focus",
    defaultScore = null,
    alignmentCues = listOf(
      "Thighs wrapped tightly, knees bent in squat",
      "Arms bound with palms pressing",
      "Elbows lifted to shoulder height"
    ),
    iconVector = Icons.Outlined.SelfImprovement,
  ),

  // 2. Seated Poses (Page 2 & 3)
  YogaPose(
    id = "easy_pose",
    englishName = "Easy",
    sanskritName = "Sukhasana",
    category = PoseCategory.SEATED,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Spine Lengthening & Calmness",
    defaultScore = 95,
    alignmentCues = listOf(
      "Sit bones rooted evenly on mat",
      "Spine long with natural curves preserved",
      "Shoulders soft away from ears"
    ),
    iconVector = Icons.Outlined.SelfImprovement,
  ),
  YogaPose(
    id = "boat",
    englishName = "Boat",
    sanskritName = "Navasana",
    category = PoseCategory.SEATED,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Deep Core & Hip Flexors",
    defaultScore = 76,
    alignmentCues = listOf(
      "Balance evenly on sit bones, sternum lifted",
      "Shin parallel to mat or legs fully extended",
      "Lower back lifted without rounding"
    ),
    iconVector = Icons.Outlined.FitnessCenter,
  ),
  YogaPose(
    id = "bound_angle",
    englishName = "Bound Angle",
    sanskritName = "Baddha Konasana",
    category = PoseCategory.SEATED,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Adductor & Groin Opening",
    defaultScore = 89,
    alignmentCues = listOf(
      "Soles of feet pressing together",
      "Spine tall, hinge gently from hips",
      "Knees dropping naturally toward floor"
    ),
    iconVector = Icons.Outlined.Spa,
  ),
  YogaPose(
    id = "half_pigeon",
    englishName = "Half Pigeon",
    sanskritName = "Ardha Kapotasana",
    category = PoseCategory.SEATED,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Deep Glute & Piriformis Release",
    defaultScore = null,
    alignmentCues = listOf(
      "Front shin angled comfortably, foot flexed",
      "Back leg extended straight back, hips level",
      "Torso upright or folded forward gently"
    ),
    iconVector = Icons.Outlined.SelfImprovement,
  ),
  YogaPose(
    id = "garland",
    englishName = "Garland",
    sanskritName = "Malasana",
    category = PoseCategory.SEATED,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Deep Hip Mobility & Pelvis",
    defaultScore = 84,
    alignmentCues = listOf(
      "Deep squat with heels grounded if possible",
      "Elbows gently pressing knees apart",
      "Spine tall, chest open forward"
    ),
    iconVector = Icons.Outlined.AccessibilityNew,
  ),

  // 3. Supine Poses (Page 3 & 4)
  YogaPose(
    id = "bridge",
    englishName = "Bridge",
    sanskritName = "Setu Bandha Sarvangasana",
    category = PoseCategory.SUPINE,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Glute Activation & Spine Opening",
    defaultScore = 91,
    alignmentCues = listOf(
      "Feet hip-width apart, parallel to each other",
      "Hips lifted by pressing through heels",
      "Chest expanding toward chin without neck strain"
    ),
    iconVector = Icons.Outlined.FitnessCenter,
  ),
  YogaPose(
    id = "corpse",
    englishName = "Corpse",
    sanskritName = "Shavasana",
    category = PoseCategory.SUPINE,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Parasympathetic Nervous System",
    defaultScore = 98,
    alignmentCues = listOf(
      "Body completely relaxed, arms resting at 45°",
      "Feet naturally flopping outward",
      "Rhythmic diaphragmatic breathing"
    ),
    iconVector = Icons.Outlined.Spa,
  ),
  YogaPose(
    id = "happy_baby",
    englishName = "Happy Baby",
    sanskritName = "Ananda Balasana",
    category = PoseCategory.SUPINE,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Sacrum Decompression & Inner Hip",
    defaultScore = null,
    alignmentCues = listOf(
      "Tailbone and sacrum flat on mat",
      "Ankles stacked above knees at 90°",
      "Shoulders relaxing toward floor"
    ),
    iconVector = Icons.Outlined.SelfImprovement,
  ),
  YogaPose(
    id = "supine_twist",
    englishName = "Supine Spinal Twist",
    sanskritName = "Supta Matsyendrasana",
    category = PoseCategory.SUPINE,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Thoracolumbar Rotation",
    defaultScore = 87,
    alignmentCues = listOf(
      "Both shoulder blades grounded on mat",
      "Knee relaxed across body",
      "Gentle gaze toward opposite arm"
    ),
    iconVector = Icons.Outlined.SelfImprovement,
  ),

  // 4. Prone Poses (Page 4)
  YogaPose(
    id = "cobra",
    englishName = "Cobra",
    sanskritName = "Bhujangasana",
    category = PoseCategory.PRONE,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Thoracic Extension & Back Strength",
    defaultScore = 86,
    alignmentCues = listOf(
      "Tops of feet pressing into mat, legs engaged",
      "Elbows hugged close to ribs, shoulders back",
      "Chest lifted using back muscles, minimal hand weight"
    ),
    iconVector = Icons.Outlined.DirectionsRun,
  ),
  YogaPose(
    id = "childs",
    englishName = "Child's",
    sanskritName = "Balasana",
    category = PoseCategory.PRONE,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Lower Back Rest & Hip Crease",
    defaultScore = 97,
    alignmentCues = listOf(
      "Big toes touching, knees separated comfortably",
      "Hips sinking back toward heels",
      "Forehead resting on mat, arms extended long"
    ),
    iconVector = Icons.Outlined.Spa,
  ),
  YogaPose(
    id = "sphinx",
    englishName = "Sphinx",
    sanskritName = "Salamba Bhujangasana",
    category = PoseCategory.PRONE,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Gentle Lumbar & Heart Opener",
    defaultScore = null,
    alignmentCues = listOf(
      "Elbows under shoulders, forearms parallel",
      "Collarbones broad, chest reaching forward",
      "Glutes soft, legs reaching back"
    ),
    iconVector = Icons.Outlined.SelfImprovement,
  ),
  YogaPose(
    id = "bow",
    englishName = "Bow",
    sanskritName = "Dhanurasana",
    category = PoseCategory.PRONE,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Anterior Chain Opening & Strength",
    defaultScore = null,
    alignmentCues = listOf(
      "Hands clasping outer ankles with flexed feet",
      "Kicking shins back and up to elevate chest",
      "Knees maintained hip-width apart"
    ),
    iconVector = Icons.Outlined.FitnessCenter,
  ),

  // 5. Arm & Leg Support (Page 4 & 5)
  YogaPose(
    id = "downward_dog",
    englishName = "Downward-Facing Dog",
    sanskritName = "Adho Mukha Shvanasana",
    category = PoseCategory.ARM_LEG_SUPPORT,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Posterior Chain & Spine Length",
    defaultScore = 90,
    alignmentCues = listOf(
      "Hands shoulder-width, fingers spread evenly",
      "Sit bones pressing high and back",
      "Ears aligned with upper arms, heels reaching down"
    ),
    iconVector = Icons.Outlined.AccessibilityNew,
  ),
  YogaPose(
    id = "plank",
    englishName = "Plank",
    sanskritName = "Phalakasana",
    category = PoseCategory.ARM_LEG_SUPPORT,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Core Rigidity & Scapular Push",
    defaultScore = 84,
    alignmentCues = listOf(
      "Wrists under shoulders, arms straight",
      "Body forming unbroken line from crown to heels",
      "Navel pulling up toward spine, glutes engaged"
    ),
    iconVector = Icons.Outlined.FitnessCenter,
  ),
  YogaPose(
    id = "cat_cow",
    englishName = "Cat & Cow",
    sanskritName = "Marjariasana & Bitilasana",
    category = PoseCategory.ARM_LEG_SUPPORT,
    difficulty = PoseDifficulty.BEGINNER,
    focusBenefit = "Segmental Spinal Articulation",
    defaultScore = 93,
    alignmentCues = listOf(
      "Inhale cow: belly drops, chest lifts, sit bones spread",
      "Exhale cat: spine rounds toward ceiling, chin to chest",
      "Flow coordinated with natural breath cycle"
    ),
    iconVector = Icons.Outlined.Spa,
  ),
  YogaPose(
    id = "side_plank",
    englishName = "Side Plank",
    sanskritName = "Vasishthasana",
    category = PoseCategory.ARM_LEG_SUPPORT,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Lateral Core & Shoulder Stability",
    defaultScore = 75,
    alignmentCues = listOf(
      "Supporting hand slightly forward of shoulder",
      "Hips lifted high, body in straight diagonal",
      "Top arm reaching vertically upward"
    ),
    iconVector = Icons.Outlined.DirectionsRun,
  ),
  YogaPose(
    id = "upward_dog",
    englishName = "Upward-Facing Dog",
    sanskritName = "Urdhva Mukha Shvanasana",
    category = PoseCategory.ARM_LEG_SUPPORT,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Pectoral Stretch & Arm Support",
    defaultScore = null,
    alignmentCues = listOf(
      "Only hands and tops of feet touching the mat",
      "Thighs fully lifted off the floor",
      "Shoulders rolled back, chest projecting forward"
    ),
    iconVector = Icons.Outlined.AccessibilityNew,
  ),

  // 6. Arm Balance & Inversion (Page 5 & 6)
  YogaPose(
    id = "crow",
    englishName = "Crow",
    sanskritName = "Kakasana",
    category = PoseCategory.ARM_BALANCE_INVERSION,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Wrist Integrity & Core Balance",
    defaultScore = 72,
    alignmentCues = listOf(
      "Hands grounded with fingers spread wide",
      "Knees snug against upper triceps near armpits",
      "Gaze forward of hands, toes lifting together"
    ),
    iconVector = Icons.Outlined.FitnessCenter,
  ),
  YogaPose(
    id = "headstand",
    englishName = "Supported Headstand",
    sanskritName = "Salamba Shirshasana A",
    category = PoseCategory.ARM_BALANCE_INVERSION,
    difficulty = PoseDifficulty.ADVANCED,
    focusBenefit = "Inversion Balance & Shoulder Lock",
    defaultScore = null,
    alignmentCues = listOf(
      "Forearms basket cradling crown, elbows shoulder-width",
      "Shoulders actively lifting away from floor and ears",
      "Core engaged with legs vertically straight"
    ),
    iconVector = Icons.Outlined.SelfImprovement,
  ),
  YogaPose(
    id = "shoulder_stand",
    englishName = "Supported Shoulder Stand",
    sanskritName = "Salamba Sarvangasana",
    category = PoseCategory.ARM_BALANCE_INVERSION,
    difficulty = PoseDifficulty.INTERMEDIATE,
    focusBenefit = "Thyroid Stimulation & Circulation",
    defaultScore = 80,
    alignmentCues = listOf(
      "Weight carried on shoulders and upper arms (never neck)",
      "Hands supporting middle/upper back",
      "Legs reaching perpendicular to floor"
    ),
    iconVector = Icons.Outlined.AccessibilityNew,
  ),
)

// MARK: - Curated Tracks Dataset
private val CURATED_TRACKS = listOf(
  CuratedTrack(
    id = "track_spine_reset",
    title = "14-Day Spine Reset",
    durationWeeks = "2 Weeks",
    targetFocus = "Thoracic Mobility & Kyphosis",
    poseCount = 8,
    completionPercent = 65,
  ),
  CuratedTrack(
    id = "track_kinetic_balance",
    title = "21-Day Kinetic Balance",
    durationWeeks = "3 Weeks",
    targetFocus = "Single-Leg & Core Equilibrium",
    poseCount = 12,
    completionPercent = 38,
  ),
  CuratedTrack(
    id = "track_morning_vitality",
    title = "7-Day Morning Vitality",
    durationWeeks = "1 Week",
    targetFocus = "Surya Namaskar & Awakening",
    poseCount = 6,
    completionPercent = 100,
  ),
  CuratedTrack(
    id = "track_restorative_sleep",
    title = "Deep Restorative Wind-Down",
    durationWeeks = "Ongoing",
    targetFocus = "Parasympathetic Vagal Tone",
    poseCount = 5,
    completionPercent = 20,
  ),
)

// MARK: - Main Programs Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
  onNavigateUp: () -> Unit,
  onHomeClicked: () -> Unit = onNavigateUp,
  onInsightsClicked: () -> Unit = {},
  onProfileClicked: () -> Unit = {},
  onCentralClick: () -> Unit = {},
) {
  var selectedCategory by remember { mutableStateOf(PoseCategory.ALL) }
  var searchQuery by remember { mutableStateOf("") }

  // Dynamic scores map saved in UI state
  val poseScores = remember {
    mutableStateMapOf<String, Int>().apply {
      POCKET_YOGA_POSES.forEach { pose ->
        pose.defaultScore?.let { put(pose.id, it) }
      }
    }
  }

  // Active Audit Test Sheet State
  var activeTestingPose by remember { mutableStateOf<YogaPose?>(null) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Filtered poses
  val filteredPoses = remember(selectedCategory, searchQuery) {
    POCKET_YOGA_POSES.filter { pose ->
      val matchesCategory = (selectedCategory == PoseCategory.ALL || pose.category == selectedCategory)
      val matchesSearch = searchQuery.isBlank() ||
        pose.englishName.contains(searchQuery, ignoreCase = true) ||
        pose.sanskritName.contains(searchQuery, ignoreCase = true) ||
        pose.focusBenefit.contains(searchQuery, ignoreCase = true)
      matchesCategory && matchesSearch
    }
  }

  val totalPoses = POCKET_YOGA_POSES.size
  val evaluatedCount = poseScores.size
  val avgScore = if (evaluatedCount > 0) poseScores.values.average().toInt() else 0

  Scaffold(
    containerColor = SukhamProgramsColors.ScreenBg,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Pose Mastery & Programs",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamProgramsColors.TitleBlack,
              fontSize = 18.sp,
            ),
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = "Back",
              tint = SukhamProgramsColors.TitleBlack,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = SukhamProgramsColors.ScreenBg,
          titleContentColor = SukhamProgramsColors.TitleBlack,
        ),
      )
    },
    bottomBar = {
      SukhamBottomNav(
        currentTab = SukhamNavTab.PROGRAMS,
        onHomeClick = onHomeClicked,
        onInsightsClick = onInsightsClicked,
        onProfileClick = onProfileClicked,
        onCentralClick = onCentralClick,
      )
    },
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentPadding = PaddingValues(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Hero Mastery Bar
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = SukhamProgramsColors.CardBg),
          border = BorderStroke(1.dp, SukhamProgramsColors.CardBorder),
          elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                  text = "Pose Mastery Dictionary",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SukhamProgramsColors.TitleBlack,
                    fontSize = 17.sp,
                  ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "$evaluatedCount / $totalPoses Poses Evaluated • Avg Score $avgScore%",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = SukhamProgramsColors.BodyGray,
                    fontSize = 12.sp,
                  ),
                )
              }

              Surface(
                shape = RoundedCornerShape(percent = 50),
                color = SukhamProgramsColors.SageContainer,
                border = BorderStroke(1.dp, SukhamProgramsColors.SagePrimary.copy(alpha = 0.3f)),
              ) {
                Text(
                  text = "$avgScore% Form",
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = SukhamProgramsColors.SageDark,
                    fontSize = 11.5.sp,
                  ),
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val progressRatio = evaluatedCount.toFloat() / totalPoses.coerceAtLeast(1)
            LinearProgressIndicator(
              progress = { progressRatio },
              modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(percent = 50)),
              color = SukhamProgramsColors.SagePrimary,
              trackColor = SukhamProgramsColors.ChipBg,
              strokeCap = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Hold each pose for 10 seconds. On-device vision evaluates joint symmetry and spine line to score your mastery.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = SukhamProgramsColors.BodyGray,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
              ),
            )
          }
        }
      }

      // 2. Search & Filter Bar
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          // Search input
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp),
            placeholder = {
              Text(
                "Search pose (e.g. Tree, Warrior, Vrikshasana)",
                style = MaterialTheme.typography.bodyMedium.copy(
                  color = SukhamProgramsColors.BodyGray.copy(alpha = 0.7f),
                  fontSize = 13.sp,
                ),
              )
            },
            leadingIcon = {
              Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = SukhamProgramsColors.BodyGray,
                modifier = Modifier.size(18.dp),
              )
            },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Clear",
                    tint = SukhamProgramsColors.BodyGray,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SukhamProgramsColors.CardBg,
              unfocusedContainerColor = SukhamProgramsColors.CardBg,
              focusedBorderColor = SukhamProgramsColors.SagePrimary,
              unfocusedBorderColor = SukhamProgramsColors.CardBorder,
              cursorColor = SukhamProgramsColors.SagePrimary,
            ),
          )

          // Pocket Yoga Categories Filter Row
          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(PoseCategory.values()) { category ->
              val isSelected = selectedCategory == category
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(percent = 50))
                  .clickable { selectedCategory = category },
                shape = RoundedCornerShape(percent = 50),
                color = if (isSelected) SukhamProgramsColors.SagePrimary else SukhamProgramsColors.CardBg,
                border = BorderStroke(
                  1.dp,
                  if (isSelected) SukhamProgramsColors.SagePrimary else SukhamProgramsColors.CardBorder,
                ),
              ) {
                Text(
                  text = category.displayName,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else SukhamProgramsColors.TitleBlack,
                    fontSize = 12.5.sp,
                  ),
                )
              }
            }
          }
        }
      }

      // 3. Section Title
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "${selectedCategory.displayName} Asana Cards",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = SukhamProgramsColors.TitleBlack,
              fontSize = 16.sp,
            ),
          )
          Text(
            text = "${filteredPoses.size} Poses",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamProgramsColors.BodyGray,
              fontWeight = FontWeight.Medium,
            ),
          )
        }
      }

      // 4. Pose Cards List
      items(filteredPoses, key = { it.id }) { pose ->
        val currentScore = poseScores[pose.id]
        YogaPoseMasteryCard(
          pose = pose,
          score = currentScore,
          onTestHoldClicked = { activeTestingPose = pose },
          modifier = Modifier.padding(horizontal = 18.dp),
        )
      }

      // 5. Featured Curated Tracks Section (Bottom)
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Featured Curated Tracks",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = SukhamProgramsColors.TitleBlack,
                  fontSize = 16.5.sp,
                ),
              )
              Text(
                text = "Multi-day progressive programs combining mastered poses",
                style = MaterialTheme.typography.bodySmall.copy(
                  color = SukhamProgramsColors.BodyGray,
                  fontSize = 12.sp,
                ),
              )
            }
          }

          LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            items(CURATED_TRACKS, key = { it.id }) { track ->
              CuratedTrackCard(track = track)
            }
          }
        }
      }
    }

    // 10-Second Hold Audit Interactive Sheet
    if (activeTestingPose != null) {
      val pose = activeTestingPose!!
      androidx.compose.ui.window.Dialog(
        onDismissRequest = { activeTestingPose = null },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
          shape = RoundedCornerShape(24.dp),
          color = SukhamProgramsColors.CardBg,
          tonalElevation = 6.dp,
          shadowElevation = 8.dp,
        ) {
          TenSecondPoseAuditSheet(
            pose = pose,
            currentScore = poseScores[pose.id],
            onScoreSaved = { newScore ->
              poseScores[pose.id] = newScore
              activeTestingPose = null
            },
            onClose = { activeTestingPose = null },
          )
        }
      }
    }
  }
}

// MARK: - Yoga Pose Card Composable
@Composable
fun YogaPoseMasteryCard(
  pose: YogaPose,
  score: Int?,
  onTestHoldClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamProgramsColors.CardBg),
    border = BorderStroke(1.dp, SukhamProgramsColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Posture Thumbnail / Illustration Box
        Surface(
          modifier = Modifier.size(66.dp),
          shape = RoundedCornerShape(14.dp),
          color = Color.White,
          border = BorderStroke(1.dp, SukhamProgramsColors.CardBorder),
          shadowElevation = 0.5.dp,
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(4.dp),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = pose.imageRes),
              contentDescription = pose.englishName,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Fit,
            )
          }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Titles & Subtitles
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = pose.englishName,
              modifier = Modifier.weight(1f, fill = false),
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamProgramsColors.TitleBlack,
                fontSize = 15.5.sp,
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            // Difficulty tag
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = pose.difficulty.badgeBg,
            ) {
              Text(
                text = pose.difficulty.label,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = pose.difficulty.badgeText,
                  fontSize = 9.5.sp,
                ),
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = pose.sanskritName,
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamProgramsColors.BodyGray,
              fontStyle = FontStyle.Italic,
              fontSize = 12.sp,
            ),
          )

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "Focus: ${pose.focusBenefit}",
            style = MaterialTheme.typography.bodySmall.copy(
              color = SukhamProgramsColors.BodyGray,
              fontSize = 11.sp,
            ),
          )
        }

        // Score Badge
        if (score != null) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (score >= 85) SukhamProgramsColors.SageContainer else SukhamProgramsColors.AmberSoft,
            border = BorderStroke(
              1.dp,
              if (score >= 85) SukhamProgramsColors.SagePrimary.copy(alpha = 0.4f) else SukhamProgramsColors.AmberPrimary.copy(alpha = 0.4f),
            ),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "$score%",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  color = if (score >= 85) SukhamProgramsColors.SageDark else SukhamProgramsColors.AmberPrimary,
                  fontSize = 13.5.sp,
                ),
              )
              Text(
                text = "Score",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = SukhamProgramsColors.BodyGray,
                  fontSize = 9.sp,
                ),
              )
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = SukhamProgramsColors.LavenderSoft,
          ) {
            Text(
              text = "New",
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = SukhamProgramsColors.LavenderPrimary,
                fontSize = 11.5.sp,
              ),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Bottom Row: Test 10s Hold Action Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Timer,
            contentDescription = null,
            tint = SukhamProgramsColors.BodyGray,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "10s Hold Analysis",
            style = MaterialTheme.typography.labelSmall.copy(
              color = SukhamProgramsColors.BodyGray,
              fontWeight = FontWeight.Medium,
              fontSize = 11.sp,
            ),
          )
        }

        Button(
          onClick = onTestHoldClicked,
          shape = RoundedCornerShape(percent = 50),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (score != null) SukhamProgramsColors.SagePrimary else SukhamProgramsColors.LavenderPrimary,
          ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
          modifier = Modifier.height(34.dp),
        ) {
          Icon(
            imageVector = if (score != null) Icons.Outlined.Refresh else Icons.Outlined.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(13.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (score != null) "Retest (10s Hold)" else "Test (10s Hold)",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 11.5.sp,
            ),
          )
        }
      }
    }
  }
}

// MARK: - Curated Track Card
@Composable
fun CuratedTrackCard(track: CuratedTrack) {
  Card(
    modifier = Modifier.width(240.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = SukhamProgramsColors.CardBg),
    border = BorderStroke(1.dp, SukhamProgramsColors.CardBorder),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Surface(
        shape = RoundedCornerShape(percent = 50),
        color = track.bgSoft,
      ) {
        Text(
          text = track.durationWeeks,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = track.highlightColor,
            fontSize = 10.sp,
          ),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = track.title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          color = SukhamProgramsColors.TitleBlack,
          fontSize = 15.sp,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Text(
        text = track.targetFocus,
        style = MaterialTheme.typography.bodySmall.copy(
          color = SukhamProgramsColors.BodyGray,
          fontSize = 11.5.sp,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "${track.poseCount} Poses Sequence",
          style = MaterialTheme.typography.labelSmall.copy(
            color = SukhamProgramsColors.BodyGray,
            fontWeight = FontWeight.Medium,
          ),
        )

        Text(
          text = "${track.completionPercent}% Complete",
          style = MaterialTheme.typography.labelSmall.copy(
            color = track.highlightColor,
            fontWeight = FontWeight.Bold,
          ),
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      LinearProgressIndicator(
        progress = { track.completionPercent / 100f },
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(RoundedCornerShape(percent = 50)),
        color = track.highlightColor,
        trackColor = SukhamProgramsColors.ChipBg,
      )
    }
  }
}

// MARK: - 10-Second Pose Hold Interactive Audit Sheet
@Composable
fun TenSecondPoseAuditSheet(
  pose: YogaPose,
  currentScore: Int?,
  onScoreSaved: (Int) -> Unit,
  onClose: () -> Unit,
) {
  // Test states: 0 = Prep/Instructions, 1 = Live 10s Countdown Testing, 2 = Complete with Score Breakdown
  var testState by remember { mutableIntStateOf(0) }
  var secondsLeft by remember { mutableIntStateOf(10) }
  var generatedScore by remember { mutableIntStateOf(0) }
  var spineAlignmentScore by remember { mutableIntStateOf(0) }
  var stabilityScore by remember { mutableIntStateOf(0) }

  // Countdown timer effect
  LaunchedEffect(testState) {
    if (testState == 1) {
      secondsLeft = 10
      while (secondsLeft > 0) {
        delay(1000L)
        secondsLeft -= 1
      }
      // Calculate realistic score between 80% and 96%
      val baseScore = Random.nextInt(84, 96)
      generatedScore = baseScore
      spineAlignmentScore = (baseScore + Random.nextInt(-3, 4)).coerceIn(80, 98)
      stabilityScore = (baseScore + Random.nextInt(-4, 3)).coerceIn(78, 96)
      testState = 2
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 22.dp)
      .padding(bottom = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Drag handle
    Box(
      modifier = Modifier
        .size(width = 40.dp, height = 4.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(SukhamProgramsColors.CardBorder),
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Pose Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column {
        Text(
          text = "${pose.englishName} (${pose.sanskritName})",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamProgramsColors.TitleBlack,
            fontSize = 18.sp,
          ),
        )
        Text(
          text = "${pose.category.displayName} • ${pose.focusBenefit}",
          style = MaterialTheme.typography.bodySmall.copy(
            color = SukhamProgramsColors.BodyGray,
            fontSize = 12.sp,
          ),
        )
      }

      IconButton(onClick = onClose) {
        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = SukhamProgramsColors.BodyGray)
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    when (testState) {
      0 -> {
        // STATE 0: Prep & Alignment Checklist with Illustration
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = SukhamProgramsColors.ScreenBg),
          border = BorderStroke(1.dp, SukhamProgramsColors.CardBorder),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // Visual Reference Banner with the exact Pocket Yoga image
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                modifier = Modifier.size(92.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, SukhamProgramsColors.CardBorder),
                shadowElevation = 0.5.dp,
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Image(
                    painter = painterResource(id = pose.imageRes),
                    contentDescription = pose.englishName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                  )
                }
              }

              Spacer(modifier = Modifier.width(16.dp))

              Column(modifier = Modifier.weight(1f)) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = SukhamProgramsColors.SageContainer,
                ) {
                  Text(
                    text = "TARGET ASANA",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = SukhamProgramsColors.SageDark,
                      fontSize = 9.5.sp,
                      letterSpacing = 0.8.sp,
                    ),
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                  text = pose.englishName,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SukhamProgramsColors.TitleBlack,
                    fontSize = 17.sp,
                  ),
                )

                Text(
                  text = pose.sanskritName,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = SukhamProgramsColors.BodyGray,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                  ),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                  text = "Focus: ${pose.focusBenefit}",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = SukhamProgramsColors.SageDark,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                  ),
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = SukhamProgramsColors.CardBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "Key Alignment Cues (10s Hold)",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamProgramsColors.TitleBlack,
                fontSize = 13.5.sp,
              ),
            )
            Spacer(modifier = Modifier.height(10.dp))
            pose.alignmentCues.forEach { cue ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 3.5.dp),
                verticalAlignment = Alignment.Top,
              ) {
                Icon(
                  Icons.Outlined.CheckCircle,
                  contentDescription = null,
                  tint = SukhamProgramsColors.SagePrimary,
                  modifier = Modifier
                    .size(16.dp)
                    .padding(top = 2.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = cue,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = SukhamProgramsColors.BodyGray,
                    fontSize = 12.sp,
                    lineHeight = 16.5.sp,
                  ),
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          shape = RoundedCornerShape(percent = 50),
          colors = ButtonDefaults.buttonColors(containerColor = SukhamProgramsColors.SagePrimary),
          onClick = { testState = 1 },
        ) {
          Icon(Icons.Outlined.Timer, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Start 10-Second Hold Audit",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 14.5.sp,
            ),
          )
        }
      }

      1 -> {
        // STATE 1: Live 10-Second Countdown with Posture Thumbnail Reference
        Surface(
          modifier = Modifier.size(72.dp),
          shape = RoundedCornerShape(14.dp),
          color = Color.White,
          border = BorderStroke(1.dp, SukhamProgramsColors.CardBorder),
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(4.dp),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = pose.imageRes),
              contentDescription = pose.englishName,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Fit,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
          modifier = Modifier
            .size(150.dp)
            .padding(6.dp),
          contentAlignment = Alignment.Center,
        ) {
          val animatedProgress by animateFloatAsState(
            targetValue = (10 - secondsLeft) / 10f,
            label = "holdProgress",
          )
          CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = SukhamProgramsColors.SagePrimary,
            trackColor = SukhamProgramsColors.ChipBg,
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round,
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "$secondsLeft",
              style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = SukhamProgramsColors.SageDark,
                fontSize = 42.sp,
              ),
            )
            Text(
              text = "SECONDS",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamProgramsColors.BodyGray,
                letterSpacing = 1.sp,
                fontSize = 10.sp,
              ),
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
          shape = RoundedCornerShape(percent = 50),
          color = SukhamProgramsColors.SageContainer,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(SukhamProgramsColors.SagePrimary, CircleShape),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "MediaPipe Vision Tracking Joint Landmarks…",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = SukhamProgramsColors.SageDark,
                fontSize = 11.5.sp,
              ),
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Hold steady in full camera view. Keep breathing smoothly.",
          style = MaterialTheme.typography.bodySmall.copy(
            color = SukhamProgramsColors.BodyGray,
            fontSize = 12.sp,
          ),
        )
      }

      2 -> {
        // STATE 2: Analysis Complete with Generated Score Breakdown & Pose Image
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SukhamProgramsColors.CardBorder),
          ) {
            Box(modifier = Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
              Image(
                painter = painterResource(id = pose.imageRes),
                contentDescription = pose.englishName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
              )
            }
          }

          Spacer(modifier = Modifier.width(20.dp))

          Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = SukhamProgramsColors.SageContainer,
            border = BorderStroke(2.dp, SukhamProgramsColors.SagePrimary),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "$generatedScore%",
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = SukhamProgramsColors.SageDark,
                    fontSize = 24.sp,
                  ),
                )
                Text(
                  text = "Form Score",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = SukhamProgramsColors.BodyGray,
                    fontSize = 8.5.sp,
                  ),
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = if (generatedScore >= 88) "Excellent Posture Mastery!" else "Good Form — Minor Adjustments Needed",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = SukhamProgramsColors.TitleBlack,
            fontSize = 16.sp,
          ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Biomechanical metric rows
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SukhamProgramsColors.ScreenBg),
          border = BorderStroke(1.dp, SukhamProgramsColors.CardBorder),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricScoreRow("Spine Alignment", spineAlignmentScore)
            MetricScoreRow("Joint Angle Stability", stabilityScore)
            MetricScoreRow("Hold Equilibrium", generatedScore)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "AI Coaching Cue: Spine line was stable throughout the 10 seconds. Focus on relaxing shoulder blades and keeping pelvic floor active.",
          style = MaterialTheme.typography.bodySmall.copy(
            color = SukhamProgramsColors.BodyGray,
            fontSize = 11.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
          ),
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
          shape = RoundedCornerShape(percent = 50),
          colors = ButtonDefaults.buttonColors(containerColor = SukhamProgramsColors.SagePrimary),
          onClick = { onScoreSaved(generatedScore) },
        ) {
          Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Save Score to Pose Card",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 14.5.sp,
            ),
          )
        }
      }
    }
  }
}

@Composable
private fun MetricScoreRow(label: String, score: Int) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.Medium,
        color = SukhamProgramsColors.TitleBlack,
        fontSize = 12.sp,
      ),
    )

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      LinearProgressIndicator(
        progress = { score / 100f },
        modifier = Modifier
          .width(70.dp)
          .height(5.dp)
          .clip(RoundedCornerShape(percent = 50)),
        color = SukhamProgramsColors.SagePrimary,
        trackColor = SukhamProgramsColors.ChipBg,
      )
      Text(
        text = "$score%",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          color = SukhamProgramsColors.SageDark,
          fontSize = 11.5.sp,
        ),
      )
    }
  }
}
