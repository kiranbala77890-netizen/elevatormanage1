package elevator.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * One row of the floor panel (Unit 5: custom Swing component + mouse events).
 * Left-click selects pickup/destination; right-click cancels the selection.
 */
public class FloorCell extends JPanel {

    private static final long serialVersionUID = 1L;

    public interface ClickHandler {
        void floorClicked(int floorIndex, boolean rightClick);
    }

    private final Floor floor;
    private final ElevatorCar car;
    private boolean hover;
    private boolean selected;

    public FloorCell(Floor floor, ElevatorCar car, ClickHandler handler) {
        this.floor = floor;
        this.car = car;
        setPreferredSize(new Dimension(300, 48));
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        setToolTipText("Click: choose pickup, then click another floor for destination. Right-click: cancel.");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handler.floorClicked(floor.getNumber(), SwingUtilities.isRightMouseButton(e));
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        Color bg = new Color(245, 245, 245);
        if (floor.getWaiting() > 0) bg = new Color(255, 236, 200);
        if (hover) bg = new Color(220, 235, 255);
        if (selected) bg = new Color(255, 245, 157);
        g2.setColor(bg);
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(190, 190, 190));
        g2.drawLine(0, h - 1, w, h - 1);

        // floor label
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        g2.drawString(floor.getLabel(), 14, h / 2 + 6);

        // waiting passengers
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        if (floor.getWaiting() > 0) {
            g2.setColor(new Color(190, 90, 0));
            g2.drawString(floor.getWaiting() + " waiting", 60, h / 2 + 4);
        } else if (selected) {
            g2.setColor(new Color(120, 100, 0));
            g2.drawString("pickup - choose destination", 60, h / 2 + 4);
        }

        // shaft + car
        int shaftX = w - 110;
        g2.setColor(new Color(210, 210, 210));
        g2.fillRect(shaftX, 2, 90, h - 4);
        if (car.getCurrentFloor() == floor.getNumber()) {
            ElevatorCar.DoorState d = car.getDoorState();
            Color body = (d == ElevatorCar.DoorState.OPEN) ? new Color(76, 175, 80)
                    : (d == ElevatorCar.DoorState.CLOSED) ? new Color(66, 66, 66) : new Color(255, 167, 38);
            g2.setColor(body);
            g2.fillRoundRect(shaftX + 6, 4, 78, h - 8, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
            g2.drawString(car.getDirection().toString(), shaftX + 12, h / 2 - 2);
            g2.drawString(car.getLoad() + "/" + car.getCapacity() + " pax", shaftX + 12, h / 2 + 12);
        }
        if (selected) {
            g2.setColor(new Color(230, 180, 0));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(1, 1, w - 3, h - 3);
        }
        g2.dispose();
    }
}
