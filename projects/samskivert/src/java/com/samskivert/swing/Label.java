//
// $Id: Label.java,v 1.25 2002/11/12 00:37:22 mdb Exp $
//
// samskivert library - useful routines for java programs
// Copyright (C) 2002 Michael Bayne
// 
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;

import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.text.AttributedString;
import java.text.AttributedCharacterIterator;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.SwingConstants;

import com.samskivert.Log;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

/**
 * The label is a multipurpose text display mechanism that can display
 * small amounts of text wrapped to fit into a variety of constrained
 * spaces. It can be requested to conform to a particular width or height
 * and will expand into the other dimension in order to accomodate the
 * text at hand. It is not a component, but is intended for use by
 * components and other more heavyweight entities.
 */
public class Label implements SwingConstants, LabelStyleConstants
{
    /**
     * Constructs a blank label.
     */
    public Label ()
    {
        this("");
    }

    /**
     * Constructs a label with the supplied text.
     */
    public Label (String text)
    {
        this(text, null, null);
    }

    /**
     * Constructs a label with the supplied text and configuration
     * parameters.
     */
    public Label (String text, Color textColor, Font font)
    {
        this(text, NORMAL, textColor, null, font);
    }

    /**
     * Constructs a label with the supplied text and configuration
     * parameters.
     */
    public Label (
        String text, int style, Color textColor, Color altColor, Font font)
    {
        setText(text);
        setStyle(style);
        setTextColor(textColor);
        setAlternateColor(altColor);
        setFont(font);
    }

    /**
     * Returns the text displayed by this label.
     */
    public String getText ()
    {
        return _text;
    }

    /**
     * Sets the text to be displayed by this label. This should be
     * followed eventually by a call to {@link #layout} and ultimately
     * {@link #render} to render the text. Simply setting the text does
     * not cause any layout to occur.
     */
    public void setText (String text)
    {
        // the Java text stuff freaks out in a variety of ways if it is
        // asked to deal with the empty string, so we fake blank labels by
        // just using a space
        if (StringUtil.blank(text)) {
            _text = " ";
        } else {
            _text = text;
        }
    }

    /**
     * Sets the font to be used by this label. If the font is not set, the
     * current font of the graphics context will be used.
     */
    public void setFont (Font font)
    {
        _font = font;
    }

    /**
     * Sets the color used to render the text.  Setting the text color to
     * <code>null</code> will render the label in the graphics context
     * color (which is the default).
     */
    public void setTextColor (Color color)
    {
        _textColor = color;
    }

    /**
     * Instructs the label to render the text with the specified alternate
     * color when rendering. The text itself will be rendered in whatever
     * color is currently set in the graphics context, but the outline or
     * shadow (if any) will always be in the specified color.
     */
    public void setAlternateColor (Color color)
    {
        _alternateColor = color;
    }

    /**
     * Returns the alignment of the text within the label.
     */
    public int getAlignment ()
    {
        return _align;
    }

    /**
     * Sets the alignment of the text within the label to either {@link
     * SwingConstants#LEFT}, {@link SwingConstants#RIGHT}, or {@link
     * SwingConstants#CENTER}. The default alignment is selected to be
     * appropriate for the locale of the text being rendered.
     */
    public void setAlignment (int align)
    {
        _align = align;
    }

    /**
     * Sets the style of the text within the label to one of the styles
     * defined in {@link LabelStyleConstants}.
     */
    public void setStyle (int style)
    {
        _style = style;
    }

    /**
     * Instructs the label to attempt to achieve a balance between width
     * and height that approximates the golden ratio (width ~1.618 times
     * height).
     */
    public void setGoldenLayout ()
    {
        // use -1 as an indicator that we should be golden
        _constraints.width = -1;
        _constraints.height = -1;
    }

    /**
     * Sets the target width for this label. Text will be wrapped to fit
     * into this width, forcibly breaking words on character boundaries if
     * a single word is too long to fit into the target width. Calling
     * this method will annul any previously established target height as
     * we must have one degree of freedom in which to maneuver.
     */
    public void setTargetWidth (int targetWidth)
    {
        if (targetWidth <= 0) {
            throw new IllegalArgumentException(
                "Invalid target width '" + targetWidth + "'");
        }
        _constraints.width = targetWidth;
        _constraints.height = 0;
    }

    /**
     * Sets the target height for this label. A simple algorithm will be
     * used to balance the width of the text in order that there are only
     * as many lines of text as fit into the target height. If rendering
     * the label as a single line of text causes it to be taller than the
     * target height, we simply render ourselves anyway. Calling this
     * method will annul any previously established target width as we
     * must have one degree of freedom in which to maneuver.
     */
    public void setTargetHeight (int targetHeight)
    {
        if (targetHeight <= 0) {
            throw new IllegalArgumentException(
                "Invalid target height '" + targetHeight + "'");
        }
        _constraints.width = 0;
        _constraints.height = targetHeight;
    }

    /**
     * Returns the number of lines used by this label.
     */
    public int getLineCount ()
    {
        return _layouts.length;
    }

    /**
     * Returns our computed dimensions. Only valid after a call to {@link
     * #layout}.
     */
    public Dimension getSize ()
    {
        return _size;
    }

    /**
     * Calls {@link #layout(Graphics2D)} with the graphics context for the
     * given component.
     */
    public void layout (Component comp)
    {
        Graphics2D gfx = (Graphics2D)comp.getGraphics();
        if (gfx != null) {
            layout(gfx);
            gfx.dispose();
        }
    }

    /**
     * Requests that this label lay out its text, obtaining information
     * from the supplied graphics context to do so. It is expected that
     * the label will be subsequently rendered in the same graphics
     * context or at least one that is configured very similarly. If not,
     * wackiness may ensue.
     */
    public void layout (Graphics2D gfx)
    {
        FontRenderContext frc = gfx.getFontRenderContext();
        ArrayList layouts = null;

        // if we have a target height, do some processing and convert that
        // into a target width
        if (_constraints.height > 0 || _constraints.width == -1) {
            int targetHeight = _constraints.height;

            // if we're approximating the golden ratio, target a height
            // that gets us near that ratio, then we can err on the side
            // of being a bit wider which is generally nicer than being
            // taller (for those of us that don't speak verticall written
            // languages, anyway)
            if (_constraints.width == -1) {
                TextLayout layout = new TextLayout(textIterator(gfx), frc);
                Rectangle2D bounds = layout.getBounds();

                int lines = 1;
                double width = bounds.getWidth()/lines;
                double height = bounds.getHeight()*lines;
                double delta = Math.abs(width/height - GOLDEN_RATIO);

                do {
                    width = bounds.getWidth() / (lines+1);
                    double nheight = bounds.getHeight() * (lines+1);
                    double ndelta = Math.abs(width/nheight - GOLDEN_RATIO);
                    if (delta <= ndelta) {
                        break;
                    }
                    delta = ndelta;
                    height = nheight;
                } while (++lines < 200); // cap ourselves at 200 lines

                targetHeight = (int)Math.ceil(height);
            }

            TextLayout layout = new TextLayout(textIterator(gfx), frc);
            Rectangle2D bounds = layout.getBounds();
            int lines = Math.round(targetHeight / getHeight(layout));
            if (lines > 1) {
                int targetWidth = (int)Math.round(bounds.getWidth() / lines);

                // attempt to lay the text out in the specified width,
                // incrementing by 10% each time; limit our attempts to 10
                // expansions to avoid infinite loops if something is fucked
                for (int i = 0; i < 10; i++) {
                    LineBreakMeasurer measurer =
                        new LineBreakMeasurer(textIterator(gfx), frc);
                    layouts = computeLines(measurer, targetWidth, _size, true);
                    if ((layouts != null) && (layouts.size() <= lines)) {
                        break;
                    }
                    targetWidth = (int)Math.round(targetWidth * 1.1);
                }
            }

        } else if (_constraints.width > 0) {
            LineBreakMeasurer measurer =
                new LineBreakMeasurer(textIterator(gfx), frc);
            layouts = computeLines(measurer, _constraints.width, _size, false);
        }

        // if no constraint, or our constraining height puts us on one line
        // then layout on one line and call it good
        if (layouts == null) {
            TextLayout layout = new TextLayout(textIterator(gfx), frc);
            Rectangle2D bounds = layout.getBounds();
            // for some reason JDK1.3 on Linux chokes on setSize(double,double)
            _size.setSize(Math.ceil(bounds.getWidth()),
                          Math.ceil(getHeight(layout)));
            layouts = new ArrayList();
            layouts.add(new Tuple(layout, bounds));
        }

        switch (_style) {
        case OUTLINE:
            // we need to be two pixels bigger in both directions
            _size.width += 2;
            _size.height += 2;
            break;

        case SHADOW:
            // we need to be one pixel bigger in both directions
            _size.width += 1;
            _size.height += 1;
            break;

        case BOLD:
            // we need to be one pixel bigger horizontally
            _size.width += 1;
            break;

        default:
            break;
        }

        // create our layouts array
        int lcount = layouts.size();
        _layouts = new TextLayout[lcount];
        _lbounds = new Rectangle2D[lcount];
        for (int ii = 0; ii < lcount; ii++) {
            Tuple tup = (Tuple)layouts.get(ii);
            _layouts[ii] = (TextLayout)tup.left;
            _lbounds[ii] = (Rectangle2D)tup.right;
        }
    }

    /**
     * Computes the lines of text for this label given the specified
     * target width. The overall size of the computed lines is stored into
     * the <code>size</code> parameter.
     *
     * @return an {@link ArrayList} or null if <code>keepWordsWhole</code>
     * was true and the lines could not be layed out in the target width.
     */
    protected ArrayList computeLines (
        LineBreakMeasurer measurer, int targetWidth, Dimension size,
        boolean keepWordsWhole)
    {
        // start with a size of zero
        double width = 0, height = 0;
        ArrayList layouts = new ArrayList();

        try {
            // obtain our new dimensions by using a line break iterator to
            // lay out our text one line at a time
            TextLayout layout;
            int lastposition = _text.length();
            while (true) {
                int nextret = _text.indexOf('\n', measurer.getPosition() + 1);
                if (nextret == -1) {
                    nextret = lastposition;
                }
                layout = measurer.nextLayout(
                    targetWidth, nextret, keepWordsWhole);
                if (layout == null) {
                    break;
                }
                Rectangle2D bounds = layout.getBounds();
                System.out.println(bounds);
                width = Math.max(width, bounds.getX() + bounds.getWidth());
                height += getHeight(layout);
                layouts.add(new Tuple(layout, bounds));
            }

            // fill in the computed size; for some reason JDK1.3 on Linux
            // chokes on setSize(double,double)
            size.setSize(Math.ceil(width), Math.ceil(height));

            // this can only happen if keepWordsWhole is true
            if (measurer.getPosition() < lastposition) {
                return null;
            }

        } catch (Throwable t) {
            Log.warning("Label layout failed [text=" + _text +
                        ", t=" + t + "].");
            Log.logStackTrace(t);
        }

        return layouts;
    }

    /**
     * Renders the layout at the specified position in the supplied
     * graphics context.
     */
    public void render (Graphics2D gfx, float x, float y)
    {
        // nothing to do if we haven't been laid out
        if (_layouts == null) {
            Log.warning("Label requested to render prior to a call " +
                        "to layout() [text=" + _text + "].");
            return;
        }

        Color old = gfx.getColor();
        if (_textColor != null) {
            gfx.setColor(_textColor);
        }

        switch (_style) {
        case OUTLINE:
            // shift everything over and down by one pixel if we're outlining
            x += 1;
            y += 1;
            break;

        case SHADOW:
            // shift everything over and up by one pixel if we're drawing
            // with a shadow
            x += 1;
            y -= 1;
            break;

        case BOLD:
            // shift everything over one pixel if we're drawing in bold
            x -= 1;
            break;

        default:
            break;
        }

        // render our text
        for (int i = 0; i < _layouts.length; i++) {
            TextLayout layout = _layouts[i];
            Rectangle2D lbounds = _lbounds[i];
            y += layout.getAscent();

            float extra = (float)(_size.width - lbounds.getWidth() -
                                  lbounds.getX());
            switch (_style) {
            case OUTLINE:
                // if we're outlining, we really have two pixels less space
                // than we think we do
                extra -= 2;
                break;

            case SHADOW:
            case BOLD:
                // if we're rendering shadowed or bolded text, we really
                // have one pixel less space than we think we do
                extra -= 1;
                break;

            default:
                break;
            }

            float rx = x;
            switch (_align) {
            case -1: rx = x + (layout.isLeftToRight() ? 0 : extra); break;
            case LEFT: break;
            case RIGHT: rx = x + extra; break;
            case CENTER: rx = x + extra/2; break;
            }

            switch (_style) {
            case OUTLINE:
                // render the outline using the hacky, but much nicer than
                // using "real" outlines (via TextLayout.getOutline), method
                Color textColor = gfx.getColor();
                gfx.setColor(_alternateColor);
                layout.draw(gfx, rx - 1, y - 1);
                layout.draw(gfx, rx - 1, y);
                layout.draw(gfx, rx - 1, y + 1);
                layout.draw(gfx, rx, y - 1);
                layout.draw(gfx, rx, y + 1);
                layout.draw(gfx, rx + 1, y - 1);
                layout.draw(gfx, rx + 1, y);
                layout.draw(gfx, rx + 1, y + 1);
                gfx.setColor(textColor);
                break;

            case SHADOW:
                textColor = gfx.getColor();
                gfx.setColor(_alternateColor);
                layout.draw(gfx, rx - 1, y + 1);
                gfx.setColor(textColor);
                break;

            case BOLD:
                layout.draw(gfx, rx + 1, y);
                break;

            default:
                break;
            }

            // and draw the text itself
            layout.draw(gfx, rx, y);

            y += layout.getDescent() + layout.getLeading();
        }

        gfx.setColor(old);
    }

    /**
     * Constructs an attributed character iterator with our text and the
     * appropriate font.
     */
    protected AttributedCharacterIterator textIterator (Graphics2D gfx)
    {
        Font font = (_font == null) ? gfx.getFont() : _font;
        HashMap map = new HashMap();
        map.put(TextAttribute.FONT, font);
        AttributedString text = new AttributedString(_text, map);
        return text.getIterator();
    }

    /**
     * Computes the height based on the leading, ascent and descent rather
     * than what the layout reports via <code>getBounds()</code> which
     * rarely seems to have any bearing on reality.
     */
    protected static float getHeight (TextLayout layout)
    {
        return layout.getLeading() + layout.getAscent() + layout.getDescent();
    }

    /** The text of the label. */
    protected String _text;

    /** The text style. */
    protected int _style;

    /** The text alignment. */
    protected int _align = -1; // -1 means default according to locale

    /** Our size constraints in either the x or y direction. */
    protected Dimension _constraints = new Dimension();

    /** Our calculated size. */
    protected Dimension _size = new Dimension();

    /** The font we use when laying out and rendering out text, or null if
     * we're to use the default font. */
    protected Font _font;

    /** Formatted text layout instances that contain each line of text. */
    protected TextLayout[] _layouts;

    /** Formatted text layout instances that contain each line of text. */
    protected Rectangle2D[] _lbounds;

    /** The color in which to render the text outline or shadow if we're
     * rendering in outline or shadow mode. */
    protected Color _alternateColor = null;

    /** The color in which to render the text or null if the text should
     * be rendered with the graphics context color. */
    protected Color _textColor = null;

    /** An approximation of the golden ratio. */
    protected static final double GOLDEN_RATIO = 1.618034;
}
