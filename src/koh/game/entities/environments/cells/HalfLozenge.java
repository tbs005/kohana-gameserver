package koh.game.entities.environments.cells;

import java.util.ArrayList;
import java.util.List;
import koh.game.entities.maps.pathfinding.MapPoint;
import koh.protocol.client.enums.DirectionsEnum;

/**
 *
 * @author Neo-Craft
 */
public class HalfLozenge implements IZone {

    public byte MinRadius;

    public byte Direction;

    public byte Radius;

    public HalfLozenge(byte minRadius, byte radius) {
        this.MinRadius = minRadius;
        this.Radius = radius;
        this.Direction = DirectionsEnum.UP;
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
    public int Surface() {
        return ((int) this.Radius * 2 + 1);
    }

    @Override
    public Short[] GetCells(short centerCell) {
        MapPoint mapPoint = MapPoint.fromCellId(centerCell);
        ArrayList<Short> list = new ArrayList<>();
        if ((int) this.MinRadius == 0) {
            list.add(centerCell);
        }
        for (int index = 1; index <= (int) this.Radius; ++index) {
            switch (this.Direction) {
                case DirectionsEnum.RIGHT:
                    HalfLozenge.AddCellIfValid(mapPoint.get_x() - index, mapPoint.get_y() + index, list);
                    HalfLozenge.AddCellIfValid(mapPoint.get_x() - index, mapPoint.get_y() - index, list);
                    break;
                case DirectionsEnum.DOWN_LEFT:
                    HalfLozenge.AddCellIfValid(mapPoint.get_x() - index, mapPoint.get_y() + index, list);
                    HalfLozenge.AddCellIfValid(mapPoint.get_x() + index, mapPoint.get_y() + index, list);
                    break;
                case DirectionsEnum.UP_LEFT:
                    HalfLozenge.AddCellIfValid(mapPoint.get_x() + index, mapPoint.get_y() + index, list);
                    HalfLozenge.AddCellIfValid(mapPoint.get_x() + index, mapPoint.get_y() - index, list);
                    break;
                case DirectionsEnum.UP_RIGHT:
                    HalfLozenge.AddCellIfValid(mapPoint.get_x() - index, mapPoint.get_y() - index, list);
                    HalfLozenge.AddCellIfValid(mapPoint.get_x() + index, mapPoint.get_y() - index, list);
                    break;
            }
        }
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
        return MinRadius;
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
