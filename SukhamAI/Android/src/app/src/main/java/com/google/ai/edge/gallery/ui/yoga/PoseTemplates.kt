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
 * Reference geometry for 78 asanas, MEASURED rather than hand-authored.
 *
 * Every row was produced by running this app's own `pose_landmarker.task` over a reference
 * photograph of the pose and computing the same twelve features [PoseClassifier] computes at
 * runtime — same model, same landmark indices, same formulas. That matters more than absolute
 * accuracy: MediaPipe's world landmarks carry their own systematic bias (a standing Tadasana
 * measures ~157 degrees at the hip rather than 180), but because the templates and the live
 * device share that bias, the comparison between them stays valid.
 *
 * The earlier hand-authored table was the source of every misclassification we hit — including
 * hip angles of 196 degrees, which an unsigned `acos` can never produce, and a sign convention
 * that had backbends and forward folds exactly the wrong way round. Measuring removed the
 * guesswork; regenerate with `extract.py` if the reference set ever changes.
 *
 * Feature order: kneeL, kneeR, hipL, hipR, elbowL, elbowR, shoulderL, shoulderR, torsoLean,
 * stance, footLift, legLean. Hips are signed — above 180 is extension (a backbend), below is
 * flexion (a fold).
 *
 * Source images: yogabreezebali.com pose guide.
 */
data class PoseTemplate(
  val name: String,
  /** Coarse family from the source guide: standing, seated, back bend, inversion, and so on. */
  val category: String,
  val features: FloatArray,
)

val POSE_TEMPLATES_78: List<PoseTemplate> =
  listOf(
      PoseTemplate(
        name = "Crane / Crow (Bakasana)",
        category = "arm balance",
        features = floatArrayOf(58f, 54f, 16f, 16f, 120f, 129f, 99f, 95f, 144f, 30f, 1f, 47f),
      ),
      PoseTemplate(
        name = "Eight Limbed Salutation (Ashtanga Namaskara)",
        category = "arm balance",
        features = floatArrayOf(151f, 135f, 141f, 131f, 60f, 70f, 9f, 21f, 110f, 35f, 0f, 83f),
      ),
      PoseTemplate(
        name = "Four-Limbed Staff (Chaturanga Dandasana)",
        category = "arm balance",
        features = floatArrayOf(169f, 175f, 172f, 192f, 92f, 90f, 22f, 25f, 88f, 34f, 4f, 87f),
      ),
      PoseTemplate(
        name = "Lotus Peacock (Padma Mayurasana)",
        category = "arm balance",
        features = floatArrayOf(42f, 47f, 136f, 160f, 111f, 126f, 35f, 32f, 119f, 25f, 7f, 80f),
      ),
      PoseTemplate(
        name = "Plank (Kumbhakasana)",
        category = "arm balance",
        features = floatArrayOf(172f, 172f, 162f, 170f, 159f, 154f, 80f, 80f, 81f, 31f, 1f, 69f),
      ),
      PoseTemplate(
        name = "Side Plank (Vasisthasana)",
        category = "arm balance",
        features = floatArrayOf(162f, 160f, 166f, 160f, 158f, 154f, 110f, 75f, 71f, 12f, 8f, 56f),
      ),
      PoseTemplate(
        name = "Upward Dog (Urdhva Mukha Svanasana)",
        category = "arm balance",
        features = floatArrayOf(154f, 169f, 216f, 227f, 176f, 154f, 35f, 20f, 32f, 48f, 2f, 79f),
      ),
      PoseTemplate(
        name = "Bow (Dhanurasana)",
        category = "back bend",
        features = floatArrayOf(96f, 116f, 244f, 256f, 169f, 173f, 68f, 66f, 37f, 74f, 8f, 144f),
      ),
      PoseTemplate(
        name = "Bridge (Setu Bandha Sarvangasana)",
        category = "back bend",
        features = floatArrayOf(73f, 83f, 210f, 222f, 165f, 161f, 46f, 60f, 133f, 46f, 0f, 52f),
      ),
      PoseTemplate(
        name = "Camel (Ustrasana)",
        category = "back bend",
        features = floatArrayOf(96f, 111f, 242f, 253f, 161f, 167f, 60f, 64f, 66f, 72f, 11f, 46f),
      ),
      PoseTemplate(
        name = "Cobra (Bhujangasana)",
        category = "back bend",
        features = floatArrayOf(152f, 165f, 222f, 230f, 155f, 148f, 27f, 21f, 28f, 46f, 0f, 83f),
      ),
      PoseTemplate(
        name = "Fish (Matsyasana)",
        category = "back bend",
        features = floatArrayOf(166f, 176f, 186f, 176f, 148f, 157f, 32f, 26f, 87f, 37f, 12f, 88f),
      ),
      PoseTemplate(
        name = "King Pigeon (Eka Pada Rajakapotasana)",
        category = "back bend",
        features = floatArrayOf(45f, 58f, 252f, 250f, 90f, 96f, 157f, 152f, 16f, 96f, 7f, 131f),
      ),
      PoseTemplate(
        name = "Raised Arm (Hasta Uttanasana)",
        category = "back bend",
        features = floatArrayOf(144f, 153f, 233f, 235f, 159f, 166f, 158f, 167f, 56f, 59f, 2f, 13f),
      ),
      PoseTemplate(
        name = "Reclined Hero (Supta Virasana)",
        category = "back bend",
        features = floatArrayOf(22f, 42f, 154f, 189f, 152f, 155f, 168f, 165f, 96f, 45f, 1f, 58f),
      ),
      PoseTemplate(
        name = "Wheel (Urdhva Dhanurasana)",
        category = "back bend",
        features = floatArrayOf(117f, 95f, 254f, 236f, 150f, 150f, 133f, 130f, 116f, 42f, 2f, 14f),
      ),
      PoseTemplate(
        name = "Wild Thing (Camatkarasana)",
        category = "back bend",
        features = floatArrayOf(158f, 129f, 228f, 217f, 163f, 145f, 123f, 164f, 89f, 50f, 17f, 38f),
      ),
      PoseTemplate(
        name = "Half Bound Lotus Forward Bend (Ardha Baddha Padmottanasana)",
        category = "forward bend",
        features = floatArrayOf(164f, 165f, 26f, 15f, 122f, 126f, 119f, 120f, 164f, 65f, 44f, 7f),
      ),
      PoseTemplate(
        name = "Head To Knee (Janu Sirsasana)",
        category = "forward bend",
        features = floatArrayOf(123f, 164f, 42f, 18f, 129f, 114f, 114f, 103f, 74f, 82f, 8f, 79f),
      ),
      PoseTemplate(
        name = "Locked Lotus (Baddha Padmasana)",
        category = "forward bend",
        features = floatArrayOf(149f, 114f, 134f, 128f, 106f, 100f, 45f, 37f, 96f, 32f, 4f, 89f),
      ),
      PoseTemplate(
        name = "Pyramid (Parsvottanasana)",
        category = "forward bend",
        features = floatArrayOf(174f, 171f, 38f, 44f, 117f, 120f, 102f, 105f, 149f, 49f, 5f, 11f),
      ),
      PoseTemplate(
        name = "Reverse Prayer Pyramid (Parsvottanasana Paschima Namaskarasana)",
        category = "forward bend",
        features = floatArrayOf(170f, 172f, 30f, 69f, 63f, 65f, 45f, 14f, 139f, 126f, 2f, 23f),
      ),
      PoseTemplate(
        name = "Sage Marichi 1 (Marichyasana I)",
        category = "forward bend",
        features = floatArrayOf(159f, 155f, 29f, 332f, 115f, 173f, 50f, 17f, 68f, 40f, 0f, 90f),
      ),
      PoseTemplate(
        name = "Seated Forward Bend (Paschimottanasana)",
        category = "forward bend",
        features = floatArrayOf(170f, 168f, 25f, 14f, 115f, 113f, 129f, 133f, 80f, 42f, 1f, 82f),
      ),
      PoseTemplate(
        name = "Standing Forward Fold (Uttanasana)",
        category = "forward bend",
        features = floatArrayOf(175f, 168f, 29f, 20f, 144f, 125f, 134f, 133f, 164f, 42f, 4f, 10f),
      ),
      PoseTemplate(
        name = "Wide Angle Seated Forward Bend (Upavistha Konasana)",
        category = "forward bend",
        features = floatArrayOf(167f, 164f, 98f, 51f, 149f, 150f, 135f, 134f, 114f, 234f, 69f, 62f),
      ),
      PoseTemplate(
        name = "Downward Dog (Adho Mukha Svanasana)",
        category = "inversion",
        features = floatArrayOf(178f, 173f, 71f, 68f, 143f, 125f, 152f, 160f, 145f, 58f, 0f, 34f),
      ),
      PoseTemplate(
        name = "Half Headstand (Urdhva Dandasana)",
        category = "inversion",
        features = floatArrayOf(178f, 178f, 80f, 84f, 68f, 112f, 147f, 134f, 169f, 36f, 3f, 92f),
      ),
      PoseTemplate(
        name = "Headstand (Sirsasana)",
        category = "inversion",
        features = floatArrayOf(166f, 174f, 181f, 193f, 97f, 108f, 101f, 99f, 175f, 28f, 2f, 175f),
      ),
      PoseTemplate(
        name = "Lotus Shoulderstand (Padma Sarvangasana)",
        category = "inversion",
        features = floatArrayOf(149f, 151f, 88f, 90f, 145f, 141f, 51f, 60f, 138f, 6f, 0f, 115f),
      ),
      PoseTemplate(
        name = "Plow (Halasana)",
        category = "inversion",
        features = floatArrayOf(176f, 174f, 69f, 71f, 164f, 177f, 84f, 97f, 175f, 40f, 2f, 68f),
      ),
      PoseTemplate(
        name = "Shoulder Stand (Sarvangasana)",
        category = "inversion",
        features = floatArrayOf(160f, 174f, 162f, 169f, 68f, 75f, 94f, 81f, 170f, 30f, 0f, 176f),
      ),
      PoseTemplate(
        name = "Super Fish (Matsyasana variation)",
        category = "inversion",
        features = floatArrayOf(177f, 176f, 120f, 124f, 162f, 172f, 77f, 72f, 74f, 36f, 0f, 131f),
      ),
      PoseTemplate(
        name = "Supported Headstand (Salamba Sirsasana)",
        category = "inversion",
        features = floatArrayOf(160f, 168f, 171f, 181f, 80f, 107f, 146f, 130f, 176f, 40f, 0f, 177f),
      ),
      PoseTemplate(
        name = "Cat (Marjariasana)",
        category = "preparatory",
        features = floatArrayOf(96f, 95f, 105f, 104f, 147f, 154f, 94f, 83f, 81f, 44f, 2f, 44f),
      ),
      PoseTemplate(
        name = "Cow (Bitilasana)",
        category = "preparatory",
        features = floatArrayOf(111f, 98f, 124f, 116f, 175f, 173f, 85f, 79f, 75f, 41f, 1f, 48f),
      ),
      PoseTemplate(
        name = "Garland / Squat (Malasana)",
        category = "preparatory",
        features = floatArrayOf(68f, 63f, 71f, 61f, 78f, 74f, 23f, 22f, 17f, 110f, 2f, 56f),
      ),
      PoseTemplate(
        name = "Table (Bharmanasana)",
        category = "preparatory",
        features = floatArrayOf(99f, 90f, 112f, 104f, 151f, 153f, 78f, 76f, 79f, 41f, 2f, 49f),
      ),
      PoseTemplate(
        name = "Child's Pose (Balasana)",
        category = "restorative",
        features = floatArrayOf(38f, 36f, 34f, 30f, 153f, 152f, 31f, 10f, 95f, 25f, 0f, 46f),
      ),
      PoseTemplate(
        name = "Corpse (Shavasana)",
        category = "restorative",
        features = floatArrayOf(165f, 167f, 159f, 199f, 169f, 164f, 53f, 35f, 120f, 81f, 18f, 109f),
      ),
      PoseTemplate(
        name = "Reclining Bound Angle (Supta Baddha Konasana)",
        category = "restorative",
        features = floatArrayOf(69f, 63f, 132f, 124f, 153f, 173f, 80f, 109f, 72f, 33f, 25f, 76f),
      ),
      PoseTemplate(
        name = "Boat (Navasana)",
        category = "seated",
        features = floatArrayOf(166f, 170f, 52f, 54f, 148f, 151f, 52f, 40f, 28f, 37f, 1f, 149f),
      ),
      PoseTemplate(
        name = "Bound Angle (Baddha Konasana)",
        category = "seated",
        features = floatArrayOf(17f, 15f, 72f, 287f, 146f, 140f, 36f, 38f, 27f, 39f, 2f, 23f),
      ),
      PoseTemplate(
        name = "Compass (Surya Yantrasana)",
        category = "seated",
        features = floatArrayOf(101f, 154f, 53f, 21f, 142f, 132f, 126f, 67f, 8f, 106f, 59f, 131f),
      ),
      PoseTemplate(
        name = "Cow Face (Gomukhasana)",
        category = "seated",
        features = floatArrayOf(83f, 87f, 94f, 119f, 112f, 74f, 157f, 49f, 8f, 100f, 11f, 70f),
      ),
      PoseTemplate(
        name = "Diamond (Vajrasana)",
        category = "seated",
        features = floatArrayOf(31f, 36f, 107f, 113f, 152f, 147f, 39f, 17f, 6f, 36f, 2f, 39f),
      ),
      PoseTemplate(
        name = "Easy Pose (Sukhasana)",
        category = "seated",
        features = floatArrayOf(43f, 51f, 72f, 64f, 124f, 120f, 33f, 33f, 7f, 20f, 3f, 75f),
      ),
      PoseTemplate(
        name = "Half Pigeon (Ardha Kapotasana)",
        category = "seated",
        features = floatArrayOf(152f, 131f, 244f, 92f, 158f, 145f, 37f, 4f, 17f, 220f, 18f, 76f),
      ),
      PoseTemplate(
        name = "Hero (Virasana)",
        category = "seated",
        features = floatArrayOf(26f, 31f, 117f, 116f, 78f, 79f, 25f, 10f, 8f, 36f, 2f, 32f),
      ),
      PoseTemplate(
        name = "Heron (Krounchasana)",
        category = "seated",
        features = floatArrayOf(22f, 28f, 93f, 94f, 88f, 112f, 92f, 90f, 12f, 42f, 11f, 51f),
      ),
      PoseTemplate(
        name = "Lotus (Padmasana)",
        category = "seated",
        features = floatArrayOf(45f, 41f, 93f, 78f, 151f, 145f, 27f, 24f, 13f, 27f, 6f, 73f),
      ),
      PoseTemplate(
        name = "Monkey / Splits (Hanumanasana)",
        category = "seated",
        features = floatArrayOf(155f, 166f, 251f, 94f, 155f, 151f, 175f, 165f, 10f, 324f, 0f, 85f),
      ),
      PoseTemplate(
        name = "Staff (Dandasana)",
        category = "seated",
        features = floatArrayOf(170f, 171f, 94f, 97f, 156f, 148f, 30f, 19f, 11f, 34f, 9f, 86f),
      ),
      PoseTemplate(
        name = "Bound Side Angle (Baddha Parsvakonasana)",
        category = "standing",
        features = floatArrayOf(126f, 174f, 326f, 168f, 94f, 115f, 56f, 17f, 62f, 217f, 1f, 51f),
      ),
      PoseTemplate(
        name = "Chair (Utkatasana)",
        category = "standing",
        features = floatArrayOf(122f, 114f, 97f, 96f, 145f, 137f, 159f, 154f, 33f, 42f, 3f, 24f),
      ),
      PoseTemplate(
        name = "Dancer (Natarajasana)",
        category = "standing",
        features = floatArrayOf(163f, 67f, 134f, 241f, 164f, 168f, 161f, 63f, 53f, 190f, 168f, 83f),
      ),
      PoseTemplate(
        name = "Eagle (Garudasana)",
        category = "standing",
        features = floatArrayOf(99f, 95f, 102f, 88f, 124f, 113f, 157f, 156f, 32f, 41f, 0f, 15f),
      ),
      PoseTemplate(
        name = "Extended Side Angle (Utthita Parsvakonasana)",
        category = "standing",
        features = floatArrayOf(145f, 159f, 314f, 164f, 142f, 163f, 74f, 158f, 67f, 214f, 9f, 51f),
      ),
      PoseTemplate(
        name = "Half Forward Bend (Ardha Uttanasana)",
        category = "standing",
        features = floatArrayOf(170f, 169f, 67f, 65f, 161f, 161f, 98f, 93f, 120f, 29f, 0f, 5f),
      ),
      PoseTemplate(
        name = "Half Lotus Tree (Ardha Padma Vrksasana)",
        category = "standing",
        features = floatArrayOf(150f, 44f, 177f, 135f, 94f, 82f, 26f, 20f, 1f, 104f, 80f, 41f),
      ),
      PoseTemplate(
        name = "Half Moon (Ardha Chandrasana)",
        category = "standing",
        features = floatArrayOf(148f, 161f, 263f, 159f, 157f, 151f, 91f, 87f, 91f, 177f, 124f, 52f),
      ),
      PoseTemplate(
        name = "Hand To Big Toe A (Utthita Hasta Padangusthasana A)",
        category = "standing",
        features = floatArrayOf(171f, 139f, 168f, 70f, 146f, 136f, 31f, 71f, 9f, 175f, 133f, 60f),
      ),
      PoseTemplate(
        name = "Hand To Big Toe B (Utthita Hasta Padangusthasana B)",
        category = "standing",
        features = floatArrayOf(156f, 167f, 164f, 282f, 122f, 160f, 40f, 85f, 22f, 211f, 144f, 62f),
      ),
      PoseTemplate(
        name = "High Lunge (Utthita Ashwa Sanchalanasana)",
        category = "standing",
        features = floatArrayOf(112f, 138f, 106f, 212f, 130f, 148f, 163f, 160f, 5f, 177f, 8f, 46f),
      ),
      PoseTemplate(
        name = "Low Lunge (Ashwa Sanchalanasana)",
        category = "standing",
        features = floatArrayOf(89f, 122f, 54f, 209f, 162f, 162f, 27f, 52f, 40f, 152f, 2f, 72f),
      ),
      PoseTemplate(
        name = "Mountain (Tadasana)",
        category = "standing",
        features = floatArrayOf(154f, 162f, 157f, 160f, 147f, 150f, 11f, 18f, 19f, 13f, 1f, 9f),
      ),
      PoseTemplate(
        name = "Revolved Triangle (Parivrtta Trikonasana)",
        category = "standing",
        features = floatArrayOf(165f, 170f, 84f, 230f, 162f, 152f, 97f, 86f, 88f, 144f, 12f, 37f),
      ),
      PoseTemplate(
        name = "Tree (Vrksasana)",
        category = "standing",
        features = floatArrayOf(157f, 45f, 178f, 119f, 83f, 83f, 27f, 26f, 7f, 88f, 69f, 16f),
      ),
      PoseTemplate(
        name = "Triangle (Utthita Trikonasana)",
        category = "standing",
        features = floatArrayOf(177f, 159f, 314f, 128f, 149f, 160f, 100f, 98f, 91f, 152f, 7f, 37f),
      ),
      PoseTemplate(
        name = "Upward Salute (Urdhva Hastasana)",
        category = "standing",
        features = floatArrayOf(174f, 175f, 192f, 199f, 155f, 154f, 156f, 159f, 11f, 36f, 3f, 8f),
      ),
      PoseTemplate(
        name = "Warrior 1 (Virabhadrasana I)",
        category = "standing",
        features = floatArrayOf(113f, 157f, 109f, 213f, 147f, 141f, 174f, 159f, 3f, 173f, 5f, 43f),
      ),
      PoseTemplate(
        name = "Warrior 2 (Virabhadrasana II)",
        category = "standing",
        features = floatArrayOf(153f, 107f, 130f, 90f, 165f, 148f, 94f, 100f, 12f, 158f, 3f, 45f),
      ),
      PoseTemplate(
        name = "Warrior 3 (Virabhadrasana III)",
        category = "standing",
        features = floatArrayOf(163f, 178f, 168f, 101f, 148f, 133f, 154f, 157f, 81f, 198f, 114f, 48f),
      ),
      PoseTemplate(
        name = "Half Spinal Twist (Ardha Matsyendrasana)",
        category = "twist",
        features = floatArrayOf(80f, 41f, 67f, 46f, 157f, 85f, 35f, 66f, 8f, 70f, 9f, 75f),
      ),
      PoseTemplate(
        name = "Lord Of The Fishes (Matsyendrasana)",
        category = "twist",
        features = floatArrayOf(101f, 152f, 61f, 82f, 152f, 80f, 32f, 58f, 11f, 91f, 46f, 103f),
      ),
      PoseTemplate(
        name = "Revolved Head To Knee (Parivrtta Janu Sirsasana)",
        category = "twist",
        features = floatArrayOf(22f, 156f, 153f, 327f, 152f, 123f, 145f, 117f, 66f, 168f, 3f, 56f),
      ),
      PoseTemplate(
        name = "Sage Marichi 3 (Marichyasana III)",
        category = "twist",
        features = floatArrayOf(43f, 164f, 60f, 102f, 119f, 83f, 13f, 42f, 13f, 104f, 2f, 84f),
      ),
      PoseTemplate(
        name = "Supine Spinal Twist (Supta Matsyendrasana)",
        category = "twist",
        features = floatArrayOf(47f, 134f, 268f, 203f, 154f, 156f, 85f, 63f, 74f, 117f, 10f, 68f),
      ),
    )
