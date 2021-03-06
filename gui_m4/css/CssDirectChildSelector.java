package gui_m4.css;

import gui_m4.elements.GuiElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by matthew on 6/30/17.
 */
public class CssDirectChildSelector extends CssCombineSelector
{
    public static final char NAME = '>';

    public CssDirectChildSelector(CssSelector left, CssSelector right)
    {
        super(left, right);
    }

    @Override
    protected List<GuiElement> combineSolve(Map<String, GuiElement> inputLeft, Map<String, GuiElement> inputRight)
    {
        List<GuiElement> output = new ArrayList<>();

        for (GuiElement pChild : inputRight.values())
        {
            GuiElement parent = pChild.getParentElement();

            if (parent != null && inputLeft.containsKey(parent.getId()))
            {
                output.add(pChild);
            }
        }

        return output;
    }
}
