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

package pixelitor.io;

import pixelitor.Composition;
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressTracker;
import pixelitor.utils.StatusBarProgressTracker;
import pixelitor.utils.SubtaskProgressTracker;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * PXC file format support.
 */
public class PXCFormat {

    private static final int CURRENT_PXC_VERSION_NUMBER = 0x03;

    // tracks the reading-writing of the whole file
    private static ProgressTracker mainPT;

    private static double workRatioForOneImage;

    private PXCFormat() {
    }

    public static Composition read(File file) throws NotPxcFormatException {
        long fileSize = file.length();
        mainPT = new StatusBarProgressTracker("Reading " + file.getName(), (int) fileSize);
        Composition comp = null;
        try (InputStream is = new ProgressTrackingInputStream(
                new FileInputStream(file), mainPT)) {
            int firstByte = is.read();
            int secondByte = is.read();
            if (firstByte == 0xAB && secondByte == 0xC4) {
                // identification bytes OK
            } else {
                throw new NotPxcFormatException(file.getName() + " is not in the pxc format.");
            }
            int versionByte = is.read();
            if (versionByte == 0) {
                throw new NotPxcFormatException(file
                        .getName() + " is in an obsolete pxc format, it can only be opened in the old beta Pixelitor versions 0.9.2-0.9.7");
            }
            if (versionByte == 1) {
                throw new NotPxcFormatException(file
                        .getName() + " is in an obsolete pxc format, it can only be opened in the old beta Pixelitor version 0.9.8");
            }
            if (versionByte == 2) {
                throw new NotPxcFormatException(file
                        .getName() + " is in an obsolete pxc format, it can only be opened in the old Pixelitor versions 0.9.9-1.1.2");
            }
            if (versionByte > 3) {
                throw new NotPxcFormatException(file.getName() + " has unknown version byte " + versionByte);
            }

            try (GZIPInputStream gs = new GZIPInputStream(is)) {
                try (ObjectInput ois = new ObjectInputStream(gs)) {
                    comp = (Composition) ois.readObject();
                    mainPT.finish();
                    mainPT = null;
                    
                    // file is transient in Composition because the pxc file can be renamed
                    comp.setFile(file);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Messages.showException(e);
        }

        return comp;
    }

    public static void write(Composition comp, File f) {
        mainPT = new StatusBarProgressTracker("Writing " + f.getName(), 100);
        int numImages = comp.calcNumImages();
        if (numImages > 0) {
            workRatioForOneImage = 1.0 / numImages;
        } else {
            workRatioForOneImage = -1;
        }
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(new byte[]{(byte) 0xAB, (byte) 0xC4, CURRENT_PXC_VERSION_NUMBER});

            try (GZIPOutputStream gz = new GZIPOutputStream(fos)) {
                try (ObjectOutput oos = new ObjectOutputStream(gz)) {
                    oos.writeObject(comp);
                    oos.flush();
                }
            }
        } catch (IOException e) {
            Messages.showException(e);
        }
        mainPT.finish();
        mainPT = null;
    }

    public static void serializeImage(ObjectOutputStream out, BufferedImage img) throws IOException {
        assert img != null;
        int imgType = img.getType();
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        out.writeInt(imgWidth);
        out.writeInt(imgHeight);
        out.writeInt(imgType);

        ProgressTracker pt = getImageTracker();

        if (imgType == BufferedImage.TYPE_BYTE_GRAY) {
            ImageIO.write(img, "PNG", out);
        } else {
            int[] pixels = ImageUtils.getPixelsAsArray(img);
            int length = pixels.length;
            int progress = 0;
            int fiveUnits = length / 20;
            for (int pixel : pixels) {
                out.writeInt(pixel);
                progress++;
                if (progress > fiveUnits) {
                    pt.unitsDone(5);
                    progress = 0;
                }
            }
        }
    }

    // when deserializing, the progress tracking
    // is done at the InputStream level, not here
    public static BufferedImage deserializeImage(ObjectInputStream in) throws IOException {
        int width = in.readInt();
        int height = in.readInt();
        int type = in.readInt();

        if (type == BufferedImage.TYPE_BYTE_GRAY) {
            return ImageIO.read(in);
        } else {
            BufferedImage img = new BufferedImage(width, height, type);
            int[] pixels = ImageUtils.getPixelsAsArray(img);

            int length = pixels.length;
            for (int i = 0; i < length; i++) {
                pixels[i] = in.readInt();
            }
            return img;
        }
    }

    private static ProgressTracker getImageTracker() {
        if (workRatioForOneImage == -1) {
            // a pxc without images
            return ProgressTracker.NULL_TRACKER;
        } else {
            return new SubtaskProgressTracker(workRatioForOneImage, mainPT);
        }
    }
}