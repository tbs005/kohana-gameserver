package koh.game.entities.environments.cells;

import java.util.ArrayList;
import java.util.List;
import koh.game.entities.maps.pathfinding.MapPoint;

/**
 *
 * @author Neo-Craft
 */
public class ZRectangle implements IZone {

    public byte Direction;

    public byte Radius;

    public byte _radius2;
    public byte _minRadius = 2;

    public boolean diagonalFree;

    public ZRectangle(byte minRadius, byte nWidth, byte nHeight) {
        this._minRadius = minRadius;
        this.Radius = nWidth;
        this._radius2 = ((nHeight != 0) ? nHeight : nWidth);
    }

    @Override
    public int Surface() {
        return (int) (Math.pow(((this.Radius + this._radius2) + 1), 2));
    }
    
      @Override
    public void SetDirection(byte Direction) {
        this.Direction = Direction;
    }

    @Override
    public void SetRadius(byte Radius) {
       this.Radius = Radius;
    }

    @Override
    public Short[] GetCells(short centerCell) {
        MapPoint origin = MapPoint.fromCellId(centerCell);
        ArrayList<Short> list = new ArrayList<>();

        int i;
        int j;
        int x = origin.get_x();
        int y = origin.get_y();
        if ((((this.Radius == 0)) || ((this._radius2 == 0)))) {
            if ((((this.MinRadius() == 0)) && (!(this.diagonalFree)))) {
                list.add(centerCell);
            };
            return list.stream().toArray(Short[]::new);
        };
        i = (x - this.Radius);
        while (i <= (x + this.Radius)) {
            j = (y - this._radius2);
            while (j <= (y + this._radius2)) {
                if ((/*(!(this._minRadius != -1)) ||*/(((Math.abs((x - i)) + Math.abs((y - j))) >= this._minRadius)))) {
                    if (((!(this.diagonalFree)) || (!((Math.abs((x - i)) == Math.abs((y - j))))))) {
                        if (MapPoint.isInMap(i, j)) {
                            this.AddCellIfValid(i, j, list);
                        };
                    };
                };
                j++;
            };
            i++;
        };

        return list.stream().toArray(Short[]::new);

    }

    private static void AddCellIfValid(int x, int y, List<Short> container) {
        if (!MapPoint.IsInMap(x, y)) {
            return;
        }
        container.add(MapPoint.CoordToCellId(x, y));
    }

    @Override
    public byte MinRadius() {
        return _minRadius;
    }

    @Override
    public byte Direction() {
        return Direction;
    }

    @Override
    public byte Radius() {
        return Radius;
    }

}