package org.bladecoder.engine.pathfinder;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.bladecoder.engine.assets.EngineAssetManager;
import org.bladecoder.engine.pathfinder.Path.Step;
import org.bladecoder.engine.util.EngineLogger;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class PixTileMap implements TileBasedMap {

	private Pixmap pixmap;
	//private boolean[][] visited;
	private float tileSize = 1;
	private String filename;
	
	public PixTileMap(String filename) {
		load(filename);
		
		//visited = new boolean[pixmap.getWidth()][pixmap.getHeight()];
		
//		for(int i=0; i<100;i++) {
//			for(int j=0; j<100;j++) {
//				int p=pixmap.getPixel(i, j);
//				System.out.print(" " + p);
//			}
//			System.out.println();
//		}
//		
//		Debug.debug(pixmap.getFormat());
	}

	@Override
	public int getWidthInTiles() {
		return pixmap.getWidth();
	}

	@Override
	public int getHeightInTiles() {
		return pixmap.getHeight();
	}

	@Override
	public void pathFinderVisited(int x, int y) {
		//visited[x][y] = true;
	}

	@Override
	public boolean blocked(Movers mover, int x, int y) {
		if(mover!=null && mover.isBlocked(x,y)) return true;
		
		//EngineLogger.debug("PIXMAP x:" + x + " y:" + y + " v:" + pixmap.getPixel(x,y));
		
		if (pixmap.getPixel(x,pixmap.getHeight()-y-1) == 255) {
			//pixmap.drawPixel( x, pixmap.getHeight()-y-1,512);
			return true;
		}
			
		return false;
	}
	
	public boolean canWalkStraight(Movers mover, Vector2 p0, Vector2 pf) {
		Vector2 tmp;
		
		if(Math.abs(p0.x-pf.x) > Math.abs(p0.y-pf.y)) {
			if(pf.x < p0.x) {
				tmp = pf;
				pf = p0;
				p0 = tmp;
			}
			
			// eq. line -> y = ax + b
			float a = (pf.y - p0.y) / (pf.x - p0.x);
			float b = p0.y - p0.x * a;
			
			for(float x = p0.x; x < pf.x; x++) {
				float y = a * x + b;
				if(blocked(mover, (int)x, (int)y)) return false;
			}
			
		} else {
			if(pf.y < p0.y) {
				tmp = pf;
				pf = p0;
				p0 = tmp;
			}
			
			// eq. line -> y = ax + b
			float a = (pf.y - p0.y) / (pf.x - p0.x);
			float b = p0.y - p0.x * a;
			
			for(float y = p0.y; y < pf.y; y++) {
				float x = (y - b) / a;
				if(blocked(mover, (int)x, (int)y)) return false;
			}			
		}
		
		return true;
	}
	
	// TODO Implement!!
	public Vector2 findNearestStraight() {
		return null;
	}
	
	public void findClosestTarget(Movers m, Vector2 p) {
		int i = 0;

		boolean found = false;

		while (!found
				&& (p.x + i < getWidthInTiles() || p.x - i > 0
						|| p.y - i > 0 || p.y + i < getHeightInTiles())) {
			i++;

			if (p.x + i < getWidthInTiles()
					&& !blocked(m, (int) (p.x + i), (int) p.y)) {
				p.x = p.x + i;
				found = true;
			} else if (p.x - i > 0
					&& !blocked(m, (int) (p.x - i), (int) p.y)) {
				p.x = p.x - i;
				found = true;
			} else if (p.y - i > 0
					&& !blocked(m, (int) p.x, (int) (p.y - i))) {
				p.y = p.y - i;
				found = true;
			} else if (p.y + i < getHeightInTiles()
					&& !blocked(m, (int) p.x, (int) (p.y + i))) {
				p.y = p.y + i;
				found = true;
			} else if (p.y + i < getHeightInTiles() && p.x + i < getWidthInTiles()
					&& !blocked(m, (int) (p.x+i), (int) (p.y + i))) {
				p.y = p.y + i;
				p.x = p.x + i;
				found = true;
			} else if (p.y + i < getHeightInTiles() && p.x - i > 0
					&& !blocked(m, (int) (p.x-i), (int) (p.y + i))) {
				p.y = p.y + i;
				p.x = p.x - i;
				found = true;
			} else if (p.y - i > 0 && p.x + i < getWidthInTiles()
					&& !blocked(m, (int) (p.x+i), (int) (p.y - i))) {
				p.y = p.y - i;
				p.x = p.x + i;
				found = true;
			}  else if (p.y - i > 0 && p.x - i > 0
					&& !blocked(m, (int) (p.x-i), (int) (p.y - i))) {
				p.y = p.y - i;
				p.x = p.x - i;
				found = true;
			}
		}
	}

	@Override
	public float getCost(Movers mover, int sx, int sy, int tx, int ty, int dirx, int diry) {
		// penalizamos el cambio de dirección
		int dx = tx-sx;
		int dy = ty - sy;
		if(dx != dirx || dy != diry) return 10;
		
		return 1;
	}
	
	public void load(String filename) {
		this.filename = filename;
		pixmap = new Pixmap(EngineAssetManager.getInstance().getAsset(filename));
	}

	public float getTileSize() {
		return tileSize;
	}

	public void setTileSize(float size) {
		this.tileSize = size;
	}
	
	public void draw(SpriteBatch batch, float w, float h) {
		Texture t = new Texture(pixmap);
		
		batch.draw(t, 0.0f, 0.0f, w, h);
	}

	public float getDepth(float x, float y) {
		float sx = x / tileSize;
		float sy = y / tileSize;
		
		int value = pixmap.getPixel((int)sx, pixmap.getHeight()-(int)sy-1);
		
		// return the red component
		return ((value & 0x00ff0000) >>> 16) / 255f;
	}
	
	public String getFilename() {
		return filename;
	}

	public void dispose() {
		pixmap.dispose();
	}
	
	public ArrayList<Vector2> findPath(Movers movers, Vector2 p0, Vector2 pf) {
		ArrayList<Vector2> walkingPath = new ArrayList<Vector2>();

		Vector2 pfScaled = new Vector2((int) (pf.x / tileSize), (int) (pf.y / tileSize));
		Vector2 p0Scaled = new Vector2((int) (p0.x / tileSize), (int) (p0.y / tileSize));
		
		// Avoid error due to rounding: The pfScaled can be equals to backgroundMap width or height and produce an indexOutbounds
		if(pfScaled.x >= getWidthInTiles()) pfScaled.x = getWidthInTiles() - 1;
		if(pfScaled.y >= getHeightInTiles()) pfScaled.y = getHeightInTiles() - 1;

		Vector2 diff = new Vector2(pf.x - pfScaled.x * tileSize, pf.y - pfScaled.y
				* tileSize);

		EngineLogger.debug(MessageFormat.format(
				"TILE MAP SIZE: {0} P0: {1} DIFF: {2}", tileSize, p0, diff));

		int maxSearchDistance = (int) (p0Scaled.dst(pfScaled) * 4);

		if (blocked(movers,
				(int) pfScaled.x, (int) pfScaled.y)) {
			findClosestTarget(movers, pfScaled);
			diff.x = 0f;
			diff.y = 0f;
		}

		if (canWalkStraight(
				movers, p0Scaled, pfScaled)) {
			EngineLogger.debug("STRAIGHT PATH FOUND!!");

			walkingPath.add(new Vector2(p0.x, p0.y));
			walkingPath.add(new Vector2(pfScaled.x * tileSize + diff.x, pfScaled.y * tileSize
					+ diff.y));

		} else {

			AStarPathFinder astar = new AStarPathFinder(this,
					maxSearchDistance, false);

			Path steps = astar.findPath(movers,
					(int) p0Scaled.x, (int) p0Scaled.y, (int) pfScaled.x,
					(int) pfScaled.y);
			StringBuilder sb = new StringBuilder();
			if (steps != null) {
				EngineLogger.debug("PATH FOUND:");

				if (steps.getLength() > 1) {

					Step s = steps.getStep(0);
					Vector2 v = new Vector2(s.getX() * tileSize + diff.x, s.getY()
							* tileSize + diff.y);
					walkingPath.add(v);

					Step nextStep = steps.getStep(1);

					boolean hor = true;
					if (nextStep.getX() == s.getX())
						hor = false;

					for (int i = 1; i < steps.getLength() - 1; i++) {
						s = nextStep;
						nextStep = steps.getStep(i + 1);

						// EngineLogger.debug(s.getX() + "," + s.getY());

						if ((hor && nextStep.getY() != s.getY())
								|| (!hor && nextStep.getX() != s.getX())) {
							v = new Vector2(s.getX() * tileSize + diff.x, s.getY()
									* tileSize + diff.y);

							sb.setLength(0);
							sb.append(v.x);
							sb.append(",");
							sb.append(v.y);
							EngineLogger.debug(sb.toString());
							walkingPath.add(v);

							hor = !hor;
						}

					}
				}

				// Add pf to the path
				Vector2 v = new Vector2(pfScaled.x * tileSize + diff.x, pfScaled.y
						* tileSize + diff.y);
				walkingPath.add(v);

				sb.setLength(0);
				sb.append(v.x);
				sb.append(",");
				sb.append(v.y);
				EngineLogger.debug(sb.toString());				
			}
		}
		
		return walkingPath;
	}	
}
