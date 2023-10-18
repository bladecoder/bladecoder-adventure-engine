/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engine.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import java.util.ArrayList;
import java.util.List;

public final class MultiFontBitmapFontData extends FreeTypeFontGenerator.FreeTypeBitmapFontData {

    private final List<FreeTypeFontGenerator.FreeTypeBitmapFontData> fallBackBitmapFontData = new ArrayList<>();
    private final List<FreeTypeFontGenerator> fallbackFontGenerators = new ArrayList<>();
    private PixmapPacker packer;

    public void addFallBackFont(FileHandle fontFile, FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        FreeTypeFontGenerator.FreeTypeBitmapFontData fallbackData = new FreeTypeFontGenerator.FreeTypeBitmapFontData();
        fallbackData.regions = regions;
        parameter.packer = packer;

        FreeTypeFontGenerator fallbackGen = new FreeTypeFontGenerator(fontFile);
        fallbackFontGenerators.add(fallbackGen);
        fallBackBitmapFontData.add(fallbackGen.generateData(parameter, fallbackData));
    }

    public void createPacker(FreeTypeFontGenerator.FreeTypeFontParameter parameter) {
        int maxTextureSize = 1024;

        int size;
        PixmapPacker.PackStrategy packStrategy;

        size = maxTextureSize;
        packStrategy = new PixmapPacker.GuillotineStrategy();

        PixmapPacker packer = new PixmapPacker(size, size, Pixmap.Format.RGBA8888, 1, false, packStrategy);
        packer.setTransparentColor(parameter.color);
        packer.getTransparentColor().a = 0;
        if (parameter.borderWidth > 0) {
            packer.setTransparentColor(parameter.borderColor);
            packer.getTransparentColor().a = 0;
        }

        this.packer = packer;
        parameter.packer = packer;
    }

    @Override
    public BitmapFont.Glyph getGlyph(char ch) {
        BitmapFont.Glyph glyph = super.getGlyph(ch);

        if (glyph == null && fallBackBitmapFontData != null) {
            for (FreeTypeFontGenerator.FreeTypeBitmapFontData data : fallBackBitmapFontData) {
                glyph = data.getGlyph(ch);
                if (glyph != null) {
                    return glyph;
                }
            }
        }

        return glyph;
    }

    @Override
    public void dispose() {
        super.dispose();
        packer.dispose();

        for (FreeTypeFontGenerator.FreeTypeBitmapFontData data : fallBackBitmapFontData) {
            data.dispose();
        }

        for (FreeTypeFontGenerator gen : fallbackFontGenerators) {
            gen.dispose();
        }
    }
}
