//
// $Id: Label.java,v 1.4 2001/12/20 02:00:12 mdb Exp $

package com.samskivert.swing;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;

import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.text.AttributedString;
import java.text.AttributedCharacterIterator;

import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.SwingConstants;

import com.samskivert.Log;

/**
 * The label is a multipurpose text display mechanism that can display
 * small amounts of text wrapped to fit into a variety of constrained
 * spaces. It can be requested to conform to a particular width or height
 * and will expand into the other dimension in order to accomodate the
 * text at hand. It is not a component, but is intended for use by
 * components and other more heavyweight entities.
 */
public class Label implements SwingConstants
{
    /**
     * Constructs a blank label.
     */
    public Label ()
    {
        setText("");
    }

    /**
     * Constructs a label with the supplied text.
     */
    public Label (String text)
    {
        setText(text);
    }

    /**
     * Sets the text to be displayed by this label. This should be
     * followed eventually by a call to {@link #layout} and ultimately
     * {@link #render} to render the text. Simply setting the text does
     * not cause any layout to occur.
     */
    public void setText (String text)
    {
        _text = text;
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
     * Sets the target width for this label. Text will be wrapped to fit
     * into this width, forcibly breaking words on character boundaries if
     * a single word is too long to fit into the target width. Calling
     * this method will annul any previously established target height as
     * we must have one degree of freedom in which to maneuver.
     */
    public void setTargetWidth (int targetWidth)
    {
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
        _constraints.width = 0;
        _constraints.height = targetHeight;
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
     * Requests that this label lay out its text, obtaining information
     * from the supplied graphics context to do so. It is expected that
     * the label will be subsequently rendered in the same graphics
     * context or at least one that is configured very similarly. If not,
     * wackiness may ensue.
     */
    public void layout (Graphics2D gfx)
    {
        Font font = (_font == null) ? gfx.getFont() : _font;
        FontRenderContext frc = gfx.getFontRenderContext();
        ArrayList layouts = null;

        // if we have a target height, do some processing and convert that
        // into a target width
        if (_constraints.height > 0) {
            TextLayout layout = new TextLayout(textIterator(gfx), frc);
            Rectangle2D bounds = layout.getBounds();
            int lines = (int)(_constraints.height / getHeight(layout));
            lines = Math.max(lines, 1);
            int targetWidth = (int)(bounds.getWidth() / lines);

            // attempt to lay the text out in the specified width,
            // incrementing by 10% each time; limit our attempts to 10
            // expansions to avoid infinite loops if something is fucked
            for (int i = 0; i < 10; i++) {
                LineBreakMeasurer measurer =
                    new LineBreakMeasurer(textIterator(gfx), frc);
                layouts = computeLines(measurer, targetWidth, _size);
                if (layouts.size() <= lines) {
                    break;
                }
                targetWidth = (int)(targetWidth * 1.1);
            }

        } else if (_constraints.width > 0) {
            LineBreakMeasurer measurer =
                new LineBreakMeasurer(textIterator(gfx), frc);
            layouts = computeLines(measurer, _constraints.width, _size);

        } else {
            // we have no target width, simply lay the text out in one big
            // fat line and call ourselves good
            TextLayout layout = new TextLayout(textIterator(gfx), frc);
            Rectangle2D bounds = layout.getBounds();
            // for some reason JDK1.3 on Linux chokes on setSize(double,double)
            _size.setSize((int)bounds.getWidth(), (int)getHeight(layout));
            layouts = new ArrayList();
            layouts.add(layout);
        }

        // create our layouts array
        _layouts = new TextLayout[layouts.size()];
        layouts.toArray(_layouts);
    }

    /**
     * Computes the lines of text for this label given the specified
     * target width. The overall size of the computed lines is stored into
     * the size parameter.
     */
    protected ArrayList computeLines (
        LineBreakMeasurer measurer, int targetWidth, Dimension size)
    {
        // start with a size of zero
        double width = 0, height = 0;

        // obtain our new dimensions by using a line break iterator to lay
        // out our text one line at a time
        ArrayList layouts = new ArrayList();
        TextLayout layout;
	while ((layout = measurer.nextLayout(targetWidth)) != null) {
            Rectangle2D bounds = layout.getBounds();
            width = Math.max(width, bounds.getWidth());
            height += getHeight(layout);
            layouts.add(layout);
	}

        // fill in the computed size; for some reason JDK1.3 on Linux
        // chokes on setSize(double,double)
        size.setSize((int)width, (int)height);

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

        // render our text
        for (int i = 0; i < _layouts.length; i++) {
            TextLayout layout = _layouts[i];
            y += layout.getAscent();

            float dx = 0, extra = (float)
                (_size.width - layout.getBounds().getWidth());
            switch (_align) {
            case -1: dx = layout.isLeftToRight() ? 0 : extra; break;
            case LEFT: dx = 0; break;
            case RIGHT: dx = extra; break;
            case CENTER: dx = extra/2; break;
            }

            layout.draw(gfx, x + dx, y);
            y += layout.getDescent() + layout.getLeading();
        }
    }

    /**
     * Constructsn an attributed character iterator with our text and the
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
}