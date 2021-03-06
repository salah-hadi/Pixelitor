/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
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

package pixelitor.compactions;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.gui.View;
import pixelitor.guides.Guides;
import pixelitor.history.CompositionReplacedEdit;
import pixelitor.history.History;
import pixelitor.layers.ContentLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.selection.SelectionActions;
import pixelitor.utils.Messages;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.util.concurrent.CompletableFuture;

import static pixelitor.Composition.ImageChangeActions.REPAINT;

/**
 * A {@link CompAction} where the processing can be simplified
 * by using the template method pattern.
 */
public abstract class SimpleCompAction extends AbstractAction implements CompAction {
    private final boolean affectsCanvasSize;

    SimpleCompAction(String name, boolean affectsCanvasSize) {
        this.affectsCanvasSize = affectsCanvasSize;
        assert name != null;
        putValue(Action.NAME, name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OpenImages.onActiveComp(this::process);
    }

    @Override
    public CompletableFuture<Composition> process(Composition oldComp) {
        View view = oldComp.getView();
        Composition newComp = oldComp.createCopy(true, true);
        Canvas newCanvas = newComp.getCanvas();
        Canvas oldCanvas = oldComp.getCanvas();

        var canvasAT = createCanvasTransform(newCanvas);
        newComp.imCoordsChanged(canvasAT, false);

        newComp.forEachLayer(this::processLayer);

        if (affectsCanvasSize) {
            changeCanvasSize(newCanvas, view);
        }

        History.add(new CompositionReplacedEdit(
                getEditName(), false, view, oldComp, newComp, canvasAT));
        view.replaceComp(newComp);
        SelectionActions.setEnabled(newComp.hasSelection(), newComp);

        Guides guides = oldComp.getGuides();
        if (guides != null) {
            Guides newGuides = createGuidesCopy(guides, view, oldCanvas);
            newComp.setGuides(newGuides);
        }

        // Only after the canvas size was updated, because
        // they are based on the canvas-sized subimage
        newComp.updateAllIconImages();

        newComp.imageChanged(REPAINT, true);
        if (affectsCanvasSize) {
            view.revalidate(); // make sure the scrollbars are OK
        }

        Messages.showInStatusBar(getStatusBarMessage());

        return CompletableFuture.completedFuture(newComp);
    }

    private void processLayer(Layer layer) {
        if (layer instanceof ContentLayer) {
            ContentLayer contentLayer = (ContentLayer) layer;
            transform(contentLayer);
        }
        if (layer.hasMask()) {
            LayerMask mask = layer.getMask();
            transform(mask);
        }
    }

    protected abstract void changeCanvasSize(Canvas newCanvas, View view);

    protected abstract String getEditName();

    /**
     * Applies the transformation to the given content layer.
     */
    protected abstract void transform(ContentLayer contentLayer);

    /**
     * Returns the change made by this action as a transform in
     * image-space coordinates relative to the canvas
     */
    protected abstract AffineTransform createCanvasTransform(Canvas canvas);

    // the oldCanvas is used by "Enlarge Canvas"
    protected abstract Guides createGuidesCopy(Guides oldGuides, View view, Canvas oldCanvas);

    protected abstract String getStatusBarMessage();
}
