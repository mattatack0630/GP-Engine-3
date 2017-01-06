package input.picker;

import rendering.Color;
import utils.math.linear.vector.Vector2f;

/**
 * Created by mjmcc on 12/3/2016.
 * <p>
 * Picking data class is used to hold information about a
 * pickable object.
 */
public class PickingData
{
	private static int idCounter = 500;

	// Unique color id
	private Vector2f cid;

	public PickingData(Vector2f cid)
	{
		this.cid = cid;
	}

	// Random Color id
	public PickingData()
	{
		this(Color.random().rg());
		//this(new Vector2f((idCounter % 255) / 255f, (idCounter / 255) / 255f));
		idCounter++;
	}

	public Vector2f getCid()
	{
		return cid;
	}
}