/*
 * Copyright 2018 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.tools.transform;

import pixelitor.utils.Cursors;

import java.awt.Cursor;

import static java.lang.String.format;
import static pixelitor.tools.util.DragDisplay.BG_WIDTH_PIXEL;
import static pixelitor.tools.util.DragDisplay.MOUSE_DISPLAY_DISTANCE;
import static pixelitor.tools.util.DragDisplay.ONE_LINER_BG_HEIGHT;

/**
 * The direction of a corner or an edge in a {@link TransformBox}.
 */
public enum Direction {
    NORTH(Cursors.N,
            -BG_WIDTH_PIXEL / 2.0f,
            -MOUSE_DISPLAY_DISTANCE),
    NORTH_WEST(Cursors.NW,
            -BG_WIDTH_PIXEL - MOUSE_DISPLAY_DISTANCE,
            -MOUSE_DISPLAY_DISTANCE),
    WEST(Cursors.W,
            -BG_WIDTH_PIXEL - MOUSE_DISPLAY_DISTANCE,
            ONE_LINER_BG_HEIGHT / 2.0f),
    SOUTH_WEST(Cursors.SW,
            -BG_WIDTH_PIXEL - MOUSE_DISPLAY_DISTANCE,
            MOUSE_DISPLAY_DISTANCE + ONE_LINER_BG_HEIGHT),
    SOUTH(Cursors.S,
            -BG_WIDTH_PIXEL / 2.0f,
            MOUSE_DISPLAY_DISTANCE + ONE_LINER_BG_HEIGHT),
    SOUTH_EAST(Cursors.SE,
            MOUSE_DISPLAY_DISTANCE,
            MOUSE_DISPLAY_DISTANCE + ONE_LINER_BG_HEIGHT),
    EAST(Cursors.E,
            MOUSE_DISPLAY_DISTANCE,
            ONE_LINER_BG_HEIGHT / 2.0f),
    NORTH_EAST(Cursors.NE,
            MOUSE_DISPLAY_DISTANCE,
            -MOUSE_DISPLAY_DISTANCE);

    // the corner offsets at 0 angle with a default transform box
    public static final int NW_OFFSET = 1;
    public static final int SW_OFFSET = 3;
    public static final int SE_OFFSET = 5;
    public static final int NE_OFFSET = 7;

    // the corner offsets at 0 angle with an "inside out" transform box,
    // where the width or the height are negative
    public static final int NW_OFFSET_IO = 7;
    public static final int SW_OFFSET_IO = 5;
    public static final int SE_OFFSET_IO = 3;
    public static final int NE_OFFSET_IO = 1;

    // the cursor that should be shown for a corner facing the current direction
    private final Cursor cursor;

    // the relative distances needed for displaying the drag display
    final float dx;
    final float dy;

    // values() creates a new array for each call, so cache it for efficiency
    private static final Direction[] dirs = values();

    Direction(Cursor cursor, float dx, float dy) {
        this.cursor = cursor;
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Returns the direction between this and the given direction
     */
    Direction getDirectionBetween(Direction other) {
        int myPos = ordinal();
        int otherPos = other.ordinal();
        if (otherPos == myPos + 2) {
            return dirs[myPos + 1];
        } else if (otherPos == myPos - 2) {
            return dirs[myPos - 1];
        } else if (myPos == 0 && otherPos == dirs.length - 2) {
            return dirs[dirs.length - 1];
        } else if (myPos == dirs.length - 2 && otherPos == 0) {
            return dirs[dirs.length - 1];
        } else if (myPos == 1 && otherPos == dirs.length - 1) {
            return dirs[0];
        } else if (myPos == dirs.length - 1 && otherPos == 1) {
            return dirs[0];
        }
        throw new IllegalStateException(format("this = %s (%d), other = %s (%d)",
                this, this.ordinal(), other, other.ordinal()));
    }

    /**
     * Return the cursor associated with the current direction
     */
    public Cursor getCursor() {
        return cursor;
    }

    /**
     * Return the direction at the given offset
     */
    public static Direction atOffset(int index) {
        assert index >= 0;
        if (index > 7) {
            index = index % 8;
        }
        return dirs[index];
    }
}
