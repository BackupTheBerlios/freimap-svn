package net.relet.freimap;

public final class Converter {

	OSMMercatorProjection projection;

	int offsetX, offsetY;
	
	public void setProjection(OSMMercatorProjection p)
	{
		projection = p;
	}

	public void setWorld(int ofsX, int ofsY)
	{
		offsetX = ofsX;
		offsetY = ofsY;
	}

	public int worldToViewX(int x) {
		return x - offsetX;
	}

	public int worldToViewY(int y) {
		return y - offsetY;
	}

	public int viewToWorldX(int x) {
		return x + offsetX;
	}

	public int viewToWorldY(int y) {
		return y + offsetY;
	}

	public int lonToWorld(double lon) {
		return (int) projection.lonToX(lon);
	}

	public int latToWorld(double lat) {
		return (int) projection.latToY(lat);
	}

	public int lonToViewX(double lon) {
		return (int) projection.lonToX(lon) - offsetX;
	}

	public int latToViewY(double lat) {
		return (int) projection.latToY(lat) - offsetY;
	}

	public double viewXToLon(int x) {
		return projection.xToLong(x + offsetX);
	}

	public double viewYToLat(int y) {
		return projection.yToLat(y + offsetY);
	}
	
	public double getScale()
	{
		return projection.getScale();
	}

}
