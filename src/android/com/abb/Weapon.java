// Copyright 2008 and onwards Matthew Burkhart and Matthew Barnes.
//
// This program is free software; you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free Software
// Foundation; version 3 of the License.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
// details.

package android.com.abb;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import java.lang.Math;
import java.util.TreeMap;
import java.util.Random;
import junit.framework.Assert;


/** The Weapon class represents a single instance of a weapon. Weapons create
 * projectiles which harm enemies. Weapons also define the projectiles they
 * generate. */
public class Weapon extends Entity {
  public Weapon(GameState game_state) {
    super();
    mGameState = game_state;  // Needed for projectile instantiation.
  }

  public void loadFromUri(Uri uri) {
    // The following map defines all of the accepted weapon and projectile
    // parameters. The parameters map is expected to populated with default
    // values letting the user override only a subset if desired within the text
    // resource at the specified uri.
    TreeMap<String, Object> parameters = new TreeMap<String, Object>();
    parameters.put(kParameterAmmo, new Integer(kDefaultAmmo));
    parameters.put(kParameterDamage, new Float(kDefaultDamage));
    parameters.put(kParameterDelay, new Float(kDefaultDelay));
    parameters.put(kParameterProjectileRectBottom, new Integer(-1));
    parameters.put(kParameterProjectileRectLeft, new Integer(-1));
    parameters.put(kParameterProjectileRectRight, new Integer(-1));
    parameters.put(kParameterProjectileRectTop, new Integer(-1));
    parameters.put(kParameterProjectileType, kDefaultProjectileType);
    parameters.put(kParameterSound, "none");
    parameters.put(kParameterSpread, new Float(kDefaultSpread));
    parameters.put(kParameterSprite, "none");
    parameters.put(kParameterTimeout, new Float(kDefaultTimeout));
    parameters.put(kParameterWeaponRectBottom, new Integer(-1));
    parameters.put(kParameterWeaponRectLeft, new Integer(-1));
    parameters.put(kParameterWeaponRectRight, new Integer(-1));
    parameters.put(kParameterWeaponRectTop, new Integer(-1));
    parameters.put(kParameterVelocity, new Float(kDefaultVelocity));
    parameters.put(kParameterVibration, new Integer(kDefaultVibration));

    // Given a fully-specified default weapon parameters map, we can parse and
    // merge in the user defined values. Note that the following method rejects
    // all keys provided by the user which were not defined above.
    String file_path = Content.getFilePath(uri);
    String[] tokens = Content.readFileTokens(file_path);
    Content.mergeKeyValueTokensWithMap(tokens, parameters);
    Content.assertIntegerNotNone(parameters, kParameterProjectileRectBottom);
    Content.assertIntegerNotNone(parameters, kParameterProjectileRectLeft);
    Content.assertIntegerNotNone(parameters, kParameterProjectileRectRight);
    Content.assertIntegerNotNone(parameters, kParameterProjectileRectTop);
    Content.assertStringNotNone(parameters, kParameterSprite);
    Content.assertIntegerNotNone(parameters, kParameterWeaponRectBottom);
    Content.assertIntegerNotNone(parameters, kParameterWeaponRectLeft);
    Content.assertIntegerNotNone(parameters, kParameterWeaponRectRight);
    Content.assertIntegerNotNone(parameters, kParameterWeaponRectTop);

    // Path names are expected to be relative to the path specified for the
    // weapon definition file.
    String uri_string = uri.toString();
    String base_uri_string =
        uri_string.substring(0, uri_string.lastIndexOf("/") + 1);

    // Now that the user defined weapon parameters have been parsed and merged,
    // we can initialize the weapon instance state accordingly.
    mAmmo = mMaxAmmo = ((Integer)parameters.get(kParameterAmmo)).intValue();
    mDamage = ((Float)parameters.get(kParameterDamage)).floatValue();
    mDelay = ((Float)parameters.get(kParameterDelay)).floatValue();
    mProjectileIsFlame = parameters.get(kParameterProjectileType).equals("flame");
    String sound = (String)parameters.get(kParameterSound);
    if (!sound.equals("none")) {
      mSoundUri = Uri.parse(base_uri_string + sound);
      mGameState.preloadSound(mSoundUri);
    } else {
      mSoundUri = null;
    }
    mSpread = ((Float)parameters.get(kParameterSpread)).floatValue();
    mTimeout = ((Float)parameters.get(kParameterTimeout)).floatValue();
    mVelocity = ((Float)parameters.get(kParameterVelocity)).floatValue();
    mVibration = ((Integer)parameters.get(kParameterVibration)).intValue();
    mSpriteUri = Uri.parse(base_uri_string + (String)parameters.get(kParameterSprite));
    sprite_rect = new Rect(
        ((Integer)parameters.get(kParameterWeaponRectLeft)).intValue(),
        ((Integer)parameters.get(kParameterWeaponRectTop)).intValue(),
        ((Integer)parameters.get(kParameterWeaponRectRight)).intValue(),
        ((Integer)parameters.get(kParameterWeaponRectBottom)).intValue());
    mProjectileRect = new Rect(
        ((Integer)parameters.get(kParameterProjectileRectLeft)).intValue(),
        ((Integer)parameters.get(kParameterProjectileRectTop)).intValue(),
        ((Integer)parameters.get(kParameterProjectileRectRight)).intValue(),
        ((Integer)parameters.get(kParameterProjectileRectBottom)).intValue());
  }

  public void setTarget(float target_x, float target_y) {
    mTargetX = target_x;
    mTargetY = target_y;
  }

  public void enableShooting(boolean shooting) {
    mShooting = shooting;
  }

  public int getAmmo() {
    return mAmmo;
  }

  public int getMaxAmmo() {
    return mMaxAmmo;
  }

  @Override
  public void step(float time_step) {
    super.step(time_step);

    // Update the shooting mechanism.
    mCurrentDelay -= time_step;
    if (mShooting && mCurrentDelay < time_step && sprite_image != -1 &&
        mAmmo > 0) {
      mAmmo -= 1;
      mCurrentDelay = mDelay;
      mPhase += 10.0f;

      float shot_distance = sprite_rect.width() / 2;
      float shot_velocity = mVelocity;
      float shot_angle = (float)Math.atan2(mTargetY, mTargetX);
      float spread_angle = mSpread * (float)Math.sin(mPhase);
      float x_offset = shot_distance * (float)Math.cos(shot_angle);
      float y_offset = shot_distance * (float)Math.sin(shot_angle);
      float dx_offset = shot_velocity * (float)Math.cos(shot_angle + spread_angle);
      float dy_offset = shot_velocity * (float)Math.sin(shot_angle + spread_angle);

      if (mProjectileIsFlame) {
        mGameState.createFireProjectile(x + x_offset, y + y_offset,
                                        dx + dx_offset, dy + dy_offset,
                                        mDamage, sprite_flipped_horizontal);
      } else {
        mGameState.createProjectile(x + x_offset, y + y_offset,
                                    dx + dx_offset, dy + dy_offset,
                                    mTimeout, mDamage,
                                    sprite_image, mProjectileRect,
                                    sprite_flipped_horizontal);
      }

      if (mVibration > 0) {
        mGameState.vibrate(mVibration);
      }
      if (mSoundUri != null) {
        mGameState.playSound(mSoundUri);
      }
    }
  }

  /** Draw the weapon position given the positions of the owners "hand"
   * positions. The coordinates are expected to be *screen coordinates*, not
   * world coordinates. */
  public void draw(Graphics graphics, float center_x, float center_y,
                   float zoom, float hand_lx, float hand_ly, float hand_rx,
                   float hand_ry) {
    // Load part image if it hasn't yet been loaded. This is necessary since the
    // graphics class must only be interacted with from the main thread. This is
    // a product of the lack of thread safety in OpenGL.
    if (mSpriteUri != null) {
      String image_path = Content.getFilePath(mSpriteUri);
      Bitmap image_bitmap = BitmapFactory.decodeFile(image_path);
      sprite_image = graphics.loadImageFromBitmap(image_bitmap);
      mSpriteUri = null;
    }

    // Note that the avatar hand coordinates are specified in screen
    // coordinates, not world coordinates.
    if (sprite_image != -1) {
      float x_offset = -sprite_rect.width() / 2.0f * zoom;
      float y_offset = -sprite_rect.height() / 2.0f * zoom;
      mDrawingMatrix.setTranslate(hand_rx, hand_ry);
      mDrawingMatrix.preRotate(
          57.2958f * (float)Math.atan2(hand_ry - hand_ly, hand_rx - hand_lx));
      mDrawingMatrix.preTranslate(x_offset, y_offset);
      mDrawingMatrix.preScale(
          zoom * sprite_rect.width(), zoom * sprite_rect.height());
      graphics.drawImage(sprite_image, sprite_rect, mDrawingMatrix,
                         false, sprite_flipped_horizontal, 1);
    }
  }

  @Override
  public Object clone() {
    return super.clone();
  }

  private int           mAmmo;
  private float         mCurrentDelay;
  private float         mDamage;
  private float         mDelay;
  private static Matrix mDrawingMatrix = new Matrix();
  private GameState     mGameState;
  private int           mMaxAmmo;
  private float         mPhase;
  private boolean       mProjectileIsFlame;
  private Rect          mProjectileRect;
  private Uri           mSoundUri;
  private boolean       mShooting;
  private float         mSpread;
  private Uri           mSpriteUri;
  private float         mTargetX;
  private float         mTargetY;
  private float         mTimeout;
  private float         mVelocity;
  private int           mVibration;

  private static final int    kDefaultAmmo           = 0;
  private static final float  kDefaultDamage         = 0.0f;
  private static final float  kDefaultDelay          = 0.2f;  // Seconds.
  private static final String kDefaultProjectileType = "normal";
  private static final float  kDefaultSpread         = 0.262f;
  private static final float  kDefaultTimeout        = 1.0f;  // Seconds.
  private static final float  kDefaultVelocity       = 60.0f;
  private static final int    kDefaultVibration      = 0;

  private static final String kParameterAmmo                 = "ammo";
  private static final String kParameterDamage               = "damage";
  private static final String kParameterDelay                = "delay";
  private static final String kParameterProjectileRectBottom = "projectile_rect_bottom";
  private static final String kParameterProjectileRectLeft   = "projectile_rect_left";
  private static final String kParameterProjectileRectRight  = "projectile_rect_right";
  private static final String kParameterProjectileRectTop    = "projectile_rect_top";
  private static final String kParameterProjectileType       = "projectile_type";
  private static final String kParameterSound                = "sound";
  private static final String kParameterSpread               = "spread";
  private static final String kParameterSprite               = "sprite";
  private static final String kParameterTimeout              = "timeout";
  private static final String kParameterWeaponRectBottom     = "weapon_rect_bottom";
  private static final String kParameterWeaponRectLeft       = "weapon_rect_left";
  private static final String kParameterWeaponRectRight      = "weapon_rect_right";
  private static final String kParameterWeaponRectTop        = "weapon_rect_top";
  private static final String kParameterVerticalSpread       = "vertical_spread";
  private static final String kParameterVelocity             = "velocity";
  private static final String kParameterVibration            = "vibration";
}
