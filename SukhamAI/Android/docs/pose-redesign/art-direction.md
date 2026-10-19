# Sukham pose illustration redesign

Generation uses the built-in image generation tool, one call per pose. Each call receives the original Pocket Yoga pose as the geometry reference and the approved Sukham Ankle Stretch illustration as the character/style reference.

## Prompt template

> Premium instructional yoga illustration: {pose name}. Image 1 is the exact pose geometry and viewing-angle reference. Preserve every limb direction, bend, grip, binding, and ground contact; never simplify this specific variation. Image 2 is character and style only, never its pose. Use the same lifelike adult woman with medium-brown skin, dark low bun, opaque matte forest-sage sports bra and leggings, and bare feet. Realistic editorial studio rendering, soft diffused light, fine fabric texture, clean white seamless backdrop, and a delicate contact shadow. Entire body uncropped, centered, filling approximately 80% of the square frame. Anatomically accurate, exactly two arms and two legs. No props, text, logos, or scenery.

Complex poses receive additional geometry notes or a correction pass when needed. Archer's pose, for example, must show the bent leg lifted with the foot held at ear height, rather than grounded.

## Files and checks

- Original geometry references: `src/app/src/main/res/drawable-nodpi/program_pose_*.png`.
- Approved replacements: sibling `program_pose_*_premium.webp` files; the approved Ankle Stretch style reference remains a PNG.
- Full-resolution generated PNGs remain in the image generation output directory.
- Android asset encoding: maximum 840 pixels, WebP quality 92, retaining the pose and composition.
- `scripts/pose_review_sheet.py` creates labeled inspection sheets without changing source images.
- `scripts/refresh_premium_pose_images.py` updates explicit Android resource references, `progress.json`, and `remaining.json`.
- `progress.json` records replacement coverage. All 568 entries are complete as of October 19, 2026; no original-image fallbacks remain.
