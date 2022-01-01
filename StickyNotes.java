package main;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.util.Timer;
import java.util.*;
import java.util.stream.*;

public class StickyNotes {

    int minWidth = 190, minHeight = 142, winX, winY, winWidth, winHeight, widthDifference, heightDifference, buttonNum;
    boolean isEntered, isPressed, isResizing;
    Color NOTE_COLOR;
    MouseListener titleTooltip;
    UndoManager undoManager = new UndoManager();
    static String language = "English";
    static int noteCount, historicCount;
    static boolean isShowOnClose = true, isAnimate = true;
    static final Rectangle screenResolution = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    static final Clipboard clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
    static final Map<StickyNotes, Boolean> notes = new HashMap<>();
    static final String dir = "src/main/";

    final JFrame app = new JFrame();
    final JDialog topBorder = new JDialog(app);
    final JDialog leftBorder = new JDialog(app);
    final JDialog rightBorder = new JDialog(app);
    final JDialog bottomBorder = new JDialog(app);
    final JPanel titleBar = new JPanel();
    final JLabel titleLabel = new JLabel();
    final JLabel addBut = new JLabel(getImage("AddBut.png"));
    final JLabel deleteBut = new JLabel();
    final JLabel settingBut = new JLabel(getImage("SettingBut.png"));
    final JLabel discardBut = new JLabel(getImage("DiscardBut.png"));
    final JTextPane note = new JTextPane();
    final JScrollPane notePane = new JScrollPane();
    final JDialog noteContext = new JDialog();
    final JPanel bottomPane = new JPanel();


    static class GradientViewport extends JViewport {
        final Color c1, c2;

        public GradientViewport(Color c1, Color c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            GradientPaint gPaint = new GradientPaint(0, 0, c1, 0, getHeight(), c2, false);
            Graphics2D g2 = (Graphics2D) g;
            g2.setPaint(gPaint);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    static class CustomScrollBarUI extends BasicScrollBarUI {
        final int SCROLL_BAR_ALPHA_ROLLOVER = 150;
        final int SCROLL_BAR_ALPHA = 100;
        final int THUMB_BORDER_SIZE = 2;
        final int THUMB_SIZE = 8;
        final Color THUMB_COLOR = Color.BLACK;

        protected JButton createDecreaseButton(int orientation) {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
        protected JButton createIncreaseButton(int orientation) {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}

        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            int alpha = isThumbRollover() ? SCROLL_BAR_ALPHA_ROLLOVER : SCROLL_BAR_ALPHA;
            int orientation = scrollbar.getOrientation();
            int arc = THUMB_SIZE;
            int x = thumbBounds.x + THUMB_BORDER_SIZE;
            int y = thumbBounds.y + THUMB_BORDER_SIZE;

            int width = orientation == JScrollBar.VERTICAL ? THUMB_SIZE : thumbBounds.width - (THUMB_BORDER_SIZE * 2);
            width = Math.max(width, THUMB_SIZE);

            int height = orientation == JScrollBar.VERTICAL ? thumbBounds.height - (THUMB_BORDER_SIZE * 2) : THUMB_SIZE;
            height = Math.max(height, THUMB_SIZE);

            Graphics2D graphics2D = (Graphics2D) g.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(new Color(THUMB_COLOR.getRed(), THUMB_COLOR.getGreen(), THUMB_COLOR.getBlue(), alpha));
            graphics2D.fillRoundRect(x, y, width, height, arc, arc);
            graphics2D.dispose();
        }
    }

    class Resizer {
        public Resizer() {
            Point location = app.getLocationOnScreen();

            topBorder.setUndecorated(true);
            topBorder.setBackground(new Color(0, 0, 0, 0));
            JLabel nwTopShadow = new JLabel(getImage("NW_Shadow.png"));
            resizeTopNW(nwTopShadow);
            topBorder.add(nwTopShadow, "West");
            JLabel nShadow = new JLabel(getImage("N_Shadow.png"));
            resizeN(nShadow);
            topBorder.add(nShadow);
            JLabel neTopShadow = new JLabel(getImage("NE_Shadow.png"));
            resizeTopNE(neTopShadow);
            topBorder.add(neTopShadow, "East");
            topBorder.setBounds(location.x - 4, location.y - 4, app.getWidth() + 8, 4);

            leftBorder.setUndecorated(true);
            leftBorder.setBackground(new Color(0, 0, 0, 0));
            JLabel nwLeftShadow = new JLabel(getImage("W_Small_Shadow.png"));
            resizeLeftNW(nwLeftShadow);
            leftBorder.add(nwLeftShadow, "North");
            JLabel wShadow = new JLabel(getImage("W_Shadow.png"));
            resizeW(wShadow);
            leftBorder.add(wShadow);
            JLabel swLeftShadow = new JLabel(new ImageIcon("src/stickyNotes/W_Small_Shadow.png"));
            resizeLeftSW(swLeftShadow);
            leftBorder.add(swLeftShadow, "South");
            leftBorder.setBounds(location.x - 5, location.y, 5, app.getHeight());

            rightBorder.setUndecorated(true);
            rightBorder.setBackground(new Color(0, 0, 0, 0));
            JLabel neRightShadow = new JLabel(getImage("E_Small_Shadow.png"));
            resizeRightNE(neRightShadow);
            rightBorder.add(neRightShadow, "North");
            JLabel eShadow = new JLabel(getImage("E_Shadow.png"));
            resizeE(eShadow);
            rightBorder.add(eShadow);
            JLabel seRightShadow = new JLabel(getImage("E_Small_Shadow.png"));
            resizeRightSE(seRightShadow);
            rightBorder.add(seRightShadow, "South");
            rightBorder.setBounds(location.x + app.getWidth(), location.y, 5, app.getHeight());

            bottomBorder.setUndecorated(true);
            bottomBorder.setBackground(new Color(0, 0, 0, 0));
            JLabel swBottomShadow = new JLabel(getImage("SW_Shadow.png"));
            resizeBottomSW(swBottomShadow);
            bottomBorder.add(swBottomShadow, "West");
            JLabel sShadow = new JLabel(getImage("S_Shadow.png"));
            resizeS(sShadow);
            bottomBorder.add(sShadow);
            JLabel seBottomShadow = new JLabel(getImage("SE_Shadow.png"));
            resizeBottomSE(seBottomShadow);
            bottomBorder.add(seBottomShadow, "East");
            bottomBorder.setBounds(location.x - 5, location.y + app.getHeight(), app.getWidth() + 10, 10);

            fadeEffect(topBorder, true);
            fadeEffect(leftBorder, true);
            fadeEffect(rightBorder, true);
            fadeEffect(bottomBorder, true);
        }

        void resizeTopNW(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    winX = app.getLocation().x;
                    winY = app.getLocation().y;
                    widthDifference = Math.abs(e.getXOnScreen() - app.getLocation().x);
                    heightDifference = e.getYOnScreen() - app.getLocation().y;
                    winWidth = app.getWidth();
                    winHeight = app.getHeight();
                }
            });
            border.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.NW_RESIZE_CURSOR);
                    int w = winWidth + winX - e.getXOnScreen() + widthDifference;
                    int h = winHeight + winY - e.getYOnScreen();
                    if (app.getWidth() == minWidth && e.getXOnScreen() >= winX - minWidth + winWidth + widthDifference) {
                        if (e.getYOnScreen() + heightDifference >= app.getLocation().y) {
                            if (app.getHeight() == minHeight)
                                app.setBounds(app.getLocation().x, app.getLocation().y, minWidth, app.getHeight());
                            else
                                app.setBounds(app.getLocation().x, e.getYOnScreen(), minWidth, h);
                        }
                        else
                            app.setBounds(app.getLocation().x, Math.min(e.getYOnScreen() + heightDifference, app.getLocation().y), minWidth, h);
                    }
                    else if (app.getWidth() != minWidth && app.getHeight() == minHeight && e.getYOnScreen() >= app.getLocation().y)
                        app.setBounds(e.getXOnScreen() - widthDifference, app.getLocation().y, w, minHeight);
                    else if (app.getHeight() == minHeight)
                        app.setBounds(e.getXOnScreen() - widthDifference, app.getLocation().y, w, h - heightDifference);
                    else
                        app.setBounds(e.getXOnScreen() - widthDifference, e.getYOnScreen() - heightDifference, w, h + heightDifference);
                }
            });
            resizerMouseListener(border, Cursor.NW_RESIZE_CURSOR);
        }

        void resizeN(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    winY = topBorder.getLocation().y;
                    winHeight = app.getHeight();
                    heightDifference = Math.abs(e.getYOnScreen() - winY);
                }
            });
            border.addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.N_RESIZE_CURSOR);
                    if (app.getHeight() == minHeight && e.getYOnScreen() > topBorder.getLocation().y + heightDifference)
                        app.setBounds(app.getLocation().x, app.getLocation().y, app.getWidth(), minHeight);
                    else
                        app.setBounds(app.getLocation().x, e.getYOnScreen() - heightDifference + 4, app.getWidth(),
                                winHeight + winY - e.getYOnScreen() + heightDifference);
                }
            });
            resizerMouseListener(border, Cursor.N_RESIZE_CURSOR);
        }

        void resizeTopNE(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    winY = app.getLocation().y;
                    widthDifference = Math.abs(e.getXOnScreen() - app.getLocation().x);
                    heightDifference = Math.abs(e.getYOnScreen() - app.getLocation().y);
                    winWidth = app.getWidth();
                    winHeight = app.getHeight();
                }
            });
            border.addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.NE_RESIZE_CURSOR);
                    int w = e.getXOnScreen() - app.getLocation().x + winWidth - widthDifference;
                    if (app.getHeight() == minHeight && e.getYOnScreen() >= app.getLocation().y)
                        app.setBounds(app.getLocation().x, app.getLocation().y, w, minHeight);
                    else
                        app.setBounds(app.getLocation().x, e.getYOnScreen() + heightDifference, w,
                                winHeight + winY - e.getYOnScreen() - heightDifference);
                }
            });
            resizerMouseListener(border, Cursor.NE_RESIZE_CURSOR);
        }

        void resizeLeftNW(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    winX = app.getLocation().x;
                    winY = app.getLocation().y;
                    widthDifference = Math.abs(e.getXOnScreen() - app.getLocation().x);
                    heightDifference = Math.abs(e.getYOnScreen() - app.getLocation().y);
                    winWidth = app.getWidth();
                    winHeight = app.getHeight();
                }
            });
            border.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.NW_RESIZE_CURSOR);
                    int w = winWidth + winX - e.getXOnScreen() - widthDifference;
                    int h = winHeight + winY - e.getYOnScreen() - heightDifference;
                    int tempH = h + heightDifference * 2;
                    if (app.getWidth() == minWidth && e.getXOnScreen() >= winX - minWidth + winWidth - widthDifference) {
                        if (e.getYOnScreen() >= app.getLocation().y) {
                            if (app.getHeight() == minHeight)
                                app.setBounds(app.getLocation().x, app.getLocation().y, minWidth - widthDifference, app.getHeight());
                            else
                                app.setBounds(app.getLocation().x, e.getYOnScreen() + heightDifference, minWidth - widthDifference, tempH);
                        }
                        else
                            app.setBounds(app.getLocation().x, Math.min(e.getYOnScreen(), app.getLocation().y), minWidth, tempH);
                    }
                    else if (app.getWidth() != minWidth && app.getHeight() == minHeight && e.getYOnScreen() >= app.getLocation().y + heightDifference)
                        app.setBounds(e.getXOnScreen() + widthDifference, app.getLocation().y, w, minHeight);
                    else if (app.getHeight() == minHeight)
                        app.setBounds(e.getXOnScreen() + widthDifference, app.getLocation().y, w, h);
                    else
                        app.setBounds(e.getXOnScreen() + widthDifference, e.getYOnScreen() - heightDifference, w, tempH);
                }
            });
            resizerMouseListener(border, Cursor.NW_RESIZE_CURSOR);
        }

        void resizeW(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    winX = app.getLocation().x;
                    winY = app.getLocation().y;
                    widthDifference = e.getXOnScreen() - app.getLocation().x;
                    winWidth = app.getWidth();
                }
            });
            border.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.W_RESIZE_CURSOR);
                    int w = winWidth + winX - e.getXOnScreen() + widthDifference;
                    if (app.getWidth() == minWidth && e.getXOnScreen() >= winX - minWidth + winWidth + widthDifference)
                        app.setBounds(winX - minWidth + winWidth, winY, minWidth, app.getHeight());
                    else
                        app.setBounds(e.getXOnScreen() - widthDifference, winY, w, app.getHeight());
                }
            });
            resizerMouseListener(border, Cursor.W_RESIZE_CURSOR);
        }

        void resizeLeftSW(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    winX = app.getLocation().x;
                    winY = app.getLocation().y;
                    widthDifference = e.getXOnScreen() - app.getLocation().x;
                    heightDifference = e.getYOnScreen() - app.getLocation().y;
                    winWidth = app.getWidth();
                    winHeight = app.getHeight();
                }
            });
            border.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.SW_RESIZE_CURSOR);
                    int w = winWidth + winX - e.getXOnScreen() + widthDifference;
                    int h = winHeight + e.getYOnScreen() - app.getLocation().y - heightDifference;
                    if (app.getHeight() == minHeight && e.getYOnScreen() <= app.getLocation().y)
                        app.setBounds(e.getXOnScreen() + widthDifference, app.getLocation().y, w, minHeight);
                    else if (app.getWidth() == minWidth && e.getXOnScreen() >= winX - minWidth + winWidth + widthDifference)
                        app.setBounds(winX - minWidth + winWidth, winY, minWidth, h);
                    else
                        app.setBounds(e.getXOnScreen() - widthDifference, winY, w, h);
                }
            });
            resizerMouseListener(border, Cursor.SW_RESIZE_CURSOR);
        }

        void resizeRightNE(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    winY = app.getLocation().y;
                    widthDifference = Math.abs(e.getXOnScreen() - app.getLocation().x - app.getWidth());
                    heightDifference = Math.abs(e.getYOnScreen() - app.getLocation().y);
                    winHeight = app.getHeight();
                }
            });
            border.addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.NE_RESIZE_CURSOR);
                    int w = e.getXOnScreen() - app.getLocation().x - widthDifference;
                    int h = winHeight + winY - e.getYOnScreen();
                    if (app.getHeight() == minHeight && e.getYOnScreen() >= app.getLocation().y + heightDifference)
                        app.setBounds(app.getLocation().x, app.getLocation().y, w, h);
                    else
                        app.setBounds(app.getLocation().x, e.getYOnScreen() - heightDifference, w, h + heightDifference);
                }
            });
            resizerMouseListener(border, Cursor.NE_RESIZE_CURSOR);
        }

        void resizeE(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    widthDifference = e.getXOnScreen() - app.getLocation().x - app.getWidth();
                }
            });
            border.addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.E_RESIZE_CURSOR);
                    app.setSize(e.getXOnScreen() - app.getLocation().x - widthDifference, app.getHeight());
                }
            });
            resizerMouseListener(border, Cursor.E_RESIZE_CURSOR);
        }

        void resizeRightSE(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    widthDifference = e.getXOnScreen() - app.getLocation().x - app.getWidth();
                    heightDifference = e.getYOnScreen() - app.getLocation().y - app.getHeight();
                }
            });
            border.addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.SE_RESIZE_CURSOR);
                    app.setSize(e.getXOnScreen() - app.getLocation().x - widthDifference,
                            e.getYOnScreen() - app.getLocation().y - heightDifference);
                }
            });
            resizerMouseListener(border, Cursor.SE_RESIZE_CURSOR);
        }

        void resizeBottomSW(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    winX = app.getLocation().x;
                    winY = app.getLocation().y;
                    widthDifference = e.getXOnScreen() - app.getLocation().x;
                    heightDifference = e.getYOnScreen() - app.getLocation().y;
                    winWidth = app.getWidth();
                    winHeight = app.getHeight();
                }
            });
            border.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.SW_RESIZE_CURSOR);
                    int w = winWidth + winX - e.getXOnScreen() + widthDifference;
                    int h = winHeight + e.getYOnScreen() - app.getLocation().y - heightDifference;
                    if (app.getHeight() == minHeight && e.getYOnScreen() <= app.getLocation().y)
                        app.setBounds(e.getXOnScreen() + widthDifference, app.getLocation().y, w, minHeight);
                    else if (app.getWidth() == minWidth && e.getXOnScreen() >= winX - minWidth + winWidth + widthDifference)
                        app.setBounds(winX - minWidth + winWidth, winY, minWidth, h);
                    else
                        app.setBounds(e.getXOnScreen() - widthDifference, winY, w, h);
                }
            });
            resizerMouseListener(border, Cursor.SW_RESIZE_CURSOR);
        }

        void resizeS(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    heightDifference = e.getYOnScreen() - app.getLocation().y - app.getHeight();
                }
            });
            border.addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.S_RESIZE_CURSOR);
                    app.setSize(app.getWidth(), e.getYOnScreen() - app.getLocation().y - heightDifference);
                    bottomBorder.setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                }
            });
            resizerMouseListener(border, Cursor.S_RESIZE_CURSOR);
        }

        public void resizeBottomSE(JLabel border) {
            border.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    isResizing = true;
                    note.requestFocus();
                    widthDifference = e.getXOnScreen() - app.getLocation().x - app.getWidth();
                    heightDifference = e.getYOnScreen() - app.getLocation().y - app.getHeight();
                }
            });
            border.addMouseMotionListener(new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    changeMouseCursor(Cursor.SE_RESIZE_CURSOR);
                    app.setSize(e.getXOnScreen() - app.getLocation().x - widthDifference,
                            e.getYOnScreen() - app.getLocation().y - heightDifference);
                }
            });
            resizerMouseListener(border, Cursor.SE_RESIZE_CURSOR);
        }

        void resizerMouseListener(JLabel border, int cursor) {
            border.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!isResizing) changeMouseCursor(cursor);
                }
                public void mouseExited(MouseEvent e) {
                    if (!isResizing) {
                        app.setCursor(Cursor.getDefaultCursor());
                        note.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                    }
                    else {
                        topBorder.setCursor(Cursor.getPredefinedCursor(cursor));
                        leftBorder.setCursor(Cursor.getPredefinedCursor(cursor));
                        rightBorder.setCursor(Cursor.getPredefinedCursor(cursor));
                        bottomBorder.setCursor(Cursor.getPredefinedCursor(cursor));
                    }
                }
                public void mouseReleased(MouseEvent e) {
                    isResizing = false;
                    app.setCursor(Cursor.getDefaultCursor());
                    note.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                    bottomPane.getComponent(0).setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    System.gc();
                }
            });
        }

        void changeMouseCursor(int cursor) {
            app.setCursor(Cursor.getPredefinedCursor(cursor));
            topBorder.setCursor(Cursor.getPredefinedCursor(cursor));
            leftBorder.setCursor(Cursor.getPredefinedCursor(cursor));
            rightBorder.setCursor(Cursor.getPredefinedCursor(cursor));
            bottomBorder.setCursor(Cursor.getPredefinedCursor(cursor));
            note.setCursor(Cursor.getPredefinedCursor(cursor));
            bottomPane.getComponent(0).setCursor(Cursor.getPredefinedCursor(cursor));
        }
    }

    class NoteContextMenu {
        int index = -1;
        final JPanel[] items = new JPanel[7];

        public NoteContextMenu() {
            noteContext.setUndecorated(true);
            noteContext.setSize(188, 241);
            noteContext.setShape(new RoundRectangle2D.Double(0, 0, 188, 241, 8, 8));
            noteContext.setLayout(null);

            String[] labels = {"Undo", "Redo", "Cut", "Copy", "Paste", "Delete", "Select All"};
            String[] accelerators = {"Ctrl+Z", "Ctrl+Y", "Ctrl+X", "Ctrl+C", "Ctrl+Y", "Del", "Ctrl+A"};
            int[] yAxis = {5, 35, 76, 106, 136, 166, 207};

            for (int i = 0; i < 7; i++) {
                JPanel item = new JPanel();
                item.setLayout(null);
                final int finalI = i;
                item.addMouseListener(new MouseAdapter() {
                    public void mouseReleased(MouseEvent e) {
                        if (item.getComponent(0).getForeground() == Color.BLACK) {
                            switch (finalI) {
                                case 0 -> undo();
                                case 1 -> redo();
                                case 2 -> cut();
                                case 3 -> copy();
                                case 4 -> paste();
                                case 5 -> delete();
                                case 6 -> selectAll();
                            }
                        }
                    }
                });
                activeMenuItem(item);
                item.setBounds(1, yAxis[i], 186, 30);

                JLabel label = new JLabel(labels[i]);
                label.setFont(new Font("Microsoft Yahei UI", Font.PLAIN, 12));
                label.setForeground(Color.BLACK);
                int h = 15 - label.getPreferredSize().height / 2;
                label.setBounds(11, h, label.getPreferredSize().width, label.getPreferredSize().height);
                item.add(label);
                JLabel accelerator = new JLabel(accelerators[i]);
                accelerator.setFont(new Font("Microsoft Yahei UI", Font.PLAIN, 12));
                accelerator.setForeground(Color.BLACK);
                Dimension size = accelerator.getPreferredSize();
                accelerator.setBounds(188 - 14 - size.width, h, size.width, size.height);
                item.add(accelerator);

                items[i] = item;
                noteContext.add(item);
            }

            JLabel noteContextBlueprint = new JLabel(getImage("NoteContext.png"));
            noteContextBlueprint.setBounds(0, 0, 188, 241);
            noteContext.add(noteContextBlueprint);


            JTextField arrow = new JTextField();
            arrow.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN)
                        index++;
                    else if (e.getKeyCode() == KeyEvent.VK_UP)
                        index--;
                    else {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER && items[index].getComponent(0).getForeground() == Color.BLACK) {
                            if (index == 0) undo();
                            else if (index == 1) redo();
                            else if (index == 2) cut();
                            else if (index == 3) copy();
                            else if (index == 4) paste();
                            else if (index == 5) delete();
                            else if (index == 6) selectAll();
                            note.requestFocus();
                        }
                        else if (e.getKeyCode() == KeyEvent.VK_U) undo();
                        else if (e.getKeyCode() == KeyEvent.VK_R) redo();
                        else if (e.getKeyCode() == KeyEvent.VK_D) delete();
                        else if (e.getKeyCode() == KeyEvent.VK_S) selectAll();

                        note.requestFocus();
                        index = -1;
                    }

                    if (index < 0) index = 6;
                    else if (index > 6) index = 0;
                    for (int i = 0; i < 7; i++) items[i].setBackground(Color.decode("#F2F2F2"));
                    items[index].setBackground(Color.decode("#DADADA"));
                }
            });
            arrow.requestFocusInWindow();
            noteContext.add(arrow);


            note.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
                        checkEnabled();
                        if (app.getLocation().x <= screenResolution.width - 188) {
                            if (app.getLocation().y <= screenResolution.height - 241)
                                noteContext.setLocation(app.getLocation().x + 11, app.getLocation().y + 37);
                            else
                                noteContext.setLocation(app.getLocation().x + 11, app.getLocation().y - 256);
                        }
                        else {
                            if (app.getLocation().y <= screenResolution.height - 241)
                                noteContext.setLocation(screenResolution.width - 163, app.getLocation().y + 37);
                            else
                                noteContext.setLocation(screenResolution.width - 163, app.getLocation().y - 256);
                        }
                        fadeEffect(noteContext, true);
                    }
                }
            });

            note.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == 3) {
                        checkEnabled();
                        if (e.getLocationOnScreen().x <= screenResolution.width - 188) {
                            if (e.getLocationOnScreen().y <= screenResolution.height - 241)
                                noteContext.setLocation(e.getXOnScreen(), e.getYOnScreen());
                            else
                                noteContext.setLocation(e.getXOnScreen(), e.getYOnScreen() - 241);
                        }
                        else {
                            if (e.getLocationOnScreen().y <= screenResolution.height - 241)
                                noteContext.setLocation(e.getXOnScreen() - 188, e.getYOnScreen());
                            else
                                noteContext.setLocation(e.getXOnScreen() - 188, e.getYOnScreen() - 241);
                        }
                        fadeEffect(noteContext, true);
                    }
                    else note.requestFocus();
                }
            });

            noteContext.addWindowFocusListener(new WindowAdapter() {
                public void windowLostFocus(WindowEvent e) {
                    for (int i = 0; i < 7; i++) items[i].setBackground(Color.decode("#F2F2F2"));
                    noteContext.setVisible(false);
                }
            });
        }

        void setEnabled(int i, boolean b) {
            if (b) {
                items[i].getComponent(0).setForeground(Color.BLACK);
                items[i].getComponent(1).setForeground(Color.BLACK);
            } else {
                items[i].getComponent(0).setForeground(Color.GRAY);
                items[i].getComponent(1).setForeground(Color.GRAY);
            }
        }

        void checkEnabled() {
            setEnabled(0, undoManager.canUndo());
            setEnabled(1, undoManager.canRedo());

            if (note.getSelectedText() != null) {
                setEnabled(2, true);
                setEnabled(3, true);
                setEnabled(5, true);
            } else {
                setEnabled(2, false);
                setEnabled(3, false);
                setEnabled(5, false);
            }
            setEnabled(6, note.getSelectionStart() != 0 || note.getSelectionEnd() != note.getText().length());
            noteContext.setAlwaysOnTop(true);

            index = -1;
        }

        void undo() {
            if (undoManager.canUndo()) {
                undoManager.undo();
                noteContext.setVisible(false);
            }
        }

        void redo() {
            if (undoManager.canRedo()) {
                undoManager.redo();
                noteContext.setVisible(false);
            }
        }

        void cut() {
            clipBoard.setContents(new StringSelection(note.getSelectedText()), null);
            try { note.getDocument().remove(note.getSelectionStart(), note.getSelectedText().length()); }
            catch (Exception e) { e.printStackTrace(); }
            if (items[2].isEnabled()) noteContext.setVisible(false);
            items[2].setEnabled(false);
        }

        void copy() {
            clipBoard.setContents(new StringSelection(note.getSelectedText()), null);
            if (items[3].isEnabled()) noteContext.setVisible(false);
            items[3].setEnabled(false);
        }

        void paste() {
            try {
                if (note.getSelectedText() != null)
                    note.getDocument().remove(note.getSelectionStart(), note.getSelectedText().length());
                note.getDocument().insertString(note.getCaretPosition(),
                        (String) clipBoard.getData(DataFlavor.stringFlavor), new SimpleAttributeSet());
            } catch (Exception exception) { exception.printStackTrace(); }
            note.requestFocus();
        }

        void delete() {
            try { note.getDocument().remove(note.getSelectionStart(), note.getSelectedText().length()); }
            catch (Exception e) { e.printStackTrace(); }
            items[5].setEnabled(false);
            noteContext.setVisible(false);
        }

        void selectAll() {
            note.setSelectionStart(0);
            note.setSelectionEnd(note.getText().length());
            items[6].setEnabled(false);
            note.requestFocus();
        }

        void activeMenuItem(JPanel item) {
            item.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    item.setBackground(Color.decode("#DADADA"));
                }
                public void mousePressed(MouseEvent e) {
                    item.setBackground(Color.decode("#DADADA"));
                }
                public void mouseExited(MouseEvent e) {
                    item.setBackground(Color.decode("#F2F2F2"));
                }
            });
        }
    }

    public StickyNotes(Color color) {
        new NoteContextMenu();
        noteCount++;
        historicCount++;
        notes.put(this, true);
        NOTE_COLOR = color;

        app.setUndecorated(true);
        app.setTitle("Untitled Note (" + historicCount + ")");
        app.setSize(210, 180);
        app.setMinimumSize(new Dimension(minWidth, minHeight));
        app.setResizable(false);
        app.setIconImage(getImage("Logo.png").getImage());
        app.setLayout(new BorderLayout());
        app.setLocation((int) ((Math.random() * (screenResolution.width - 240)) + 40),
                (int) ((Math.random() * (screenResolution.height - 240)) + 40));
        changeAppIcon();


        if (NOTE_COLOR == Color.YELLOW) titleBar.setBackground(Color.decode("#F8F7B6"));
        else if (NOTE_COLOR == Color.BLUE) titleBar.setBackground(Color.decode("#C9ECF8"));
        else if (NOTE_COLOR == Color.GREEN) titleBar.setBackground(Color.decode("#C5F7C1"));
        else if (NOTE_COLOR == Color.PINK) titleBar.setBackground(Color.decode("#F1C3F1"));
        else if (NOTE_COLOR == Color.MAGENTA) titleBar.setBackground(Color.decode("#D4CDF3"));
        else titleBar.setBackground(Color.decode("#F5F5F5"));

        titleBar.setPreferredSize(new Dimension(190, 28));
        titleBar.setMinimumSize(new Dimension(190, 28));
        titleBar.setMaximumSize(new Dimension(190, 28));
        titleBar.setLayout(null);
        titleBar.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!app.isFocused()) {
                    addBut.setVisible(true);
                    deleteBut.setVisible(true);
                    settingBut.setVisible(true);
                }
            }
            public void mouseExited(MouseEvent e) {
                if (!app.isFocused()) {
                    addBut.setVisible(false);
                    deleteBut.setVisible(false);
                    settingBut.setVisible(false);
                }
            }
            public void mousePressed(MouseEvent e) {
                winX = e.getX();
                winY = e.getY();
                noteContext.setVisible(false);
                buttonNum = e.getButton();
            }
            public void mouseReleased(MouseEvent e) {
                winX = app.getLocation().x;
                winY = app.getLocation().y;
                winWidth = app.getWidth();
                winHeight = app.getHeight();
                if (app.getLocation().y < 0) app.setLocation(app.getLocation().x, 0);
                System.gc();
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (buttonNum == 1) {
                    GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    PointerInfo pointer = MouseInfo.getPointerInfo();
                    if (pointer.getLocation().y >= environment.getMaximumWindowBounds().height - 2) {
                        for (GraphicsDevice device : environment.getScreenDevices()) {
                            try {
                                Robot r = new Robot(device);
                                r.mouseMove(pointer.getLocation().x, environment.getMaximumWindowBounds().height - 2);
                                app.setLocation(e.getXOnScreen() - winX, app.getLocation().y);
                            } catch (Exception ignore) {
                            }
                        }
                    } else app.setLocation(e.getXOnScreen() - winX, e.getYOnScreen() - winY);

                    Point location = app.getLocationOnScreen();
                    topBorder.setLocation(location.x - 5, location.y - 4);
                    leftBorder.setLocation(location.x - 5, location.y);
                    rightBorder.setLocation(location.x + app.getWidth(), location.y);
                    bottomBorder.setLocation(location.x - 5, location.y + app.getHeight());
                }
            }
        });
        titleBar.add(titleLabel);
        app.add(titleBar, BorderLayout.NORTH);


        addBut.setName("AddBut");
        addBut.addMouseListener(createToolTip(addBut, "New Note (Ctrl+N)"));
        addBut.setBounds(8, 5, 18, 18);
        titleBar.add(addBut);


        deleteBut.setName("DeleteBut");
        changeButtonIcon(deleteBut,  "");
        deleteBut.addMouseListener(createToolTip(deleteBut, "Delete Note (Ctrl+D)"));
        deleteBut.setBounds(app.getWidth() - 26, 5, 18, 18);
        titleBar.add(deleteBut);


        settingBut.setName("SettingBut");
        settingBut.addMouseListener(createToolTip(settingBut, "Settings (Ctrl+S)"));
        settingBut.setBounds(app.getWidth() - 44, 4, 18, 18);
        titleBar.add(settingBut);


        note.setOpaque(false);
        note.setFont(new Font("Segoe Print", Font.PLAIN, 15));
        note.setMargin(new Insets(5, 6, 2, 6));
        note.getInputMap().put(KeyStroke.getKeyStroke("control N"), "new");
        note.getActionMap().put("new", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                new StickyNotes(NOTE_COLOR).show();
            }
        });
        note.getInputMap().put(KeyStroke.getKeyStroke("control D"), "delete");
        note.getActionMap().put("delete", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                deleteNoteDialog();
            }
        });
        note.getInputMap().put(KeyStroke.getKeyStroke("control S"), "setting");
        note.getActionMap().put("setting", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                settingDialog();
            }
        });
        note.getInputMap().put(KeyStroke.getKeyStroke("alt F4"), "close");
        note.getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveNotes();
                System.exit(0);
            }
        });


        GradientViewport gradient;
        if (NOTE_COLOR == Color.YELLOW) gradient = new GradientViewport(Color.decode("#FDFDCA"), Color.decode("#FCF9A3"));
        else if (NOTE_COLOR == Color.BLUE) gradient = new GradientViewport(Color.decode("#D9F3FB"), Color.decode("#B8DBF4"));
        else if (NOTE_COLOR == Color.GREEN) gradient = new GradientViewport(Color.decode("#D2FFCC"), Color.decode("#B1E8AE"));
        else if (NOTE_COLOR == Color.PINK) gradient = new GradientViewport(Color.decode("#F6D3F6"), Color.decode("#EBAEEB"));
        else if (NOTE_COLOR == Color.MAGENTA) gradient = new GradientViewport(Color.decode("#DDD9FE"), Color.decode("#C6B8FE"));
        else gradient = new GradientViewport(Color.decode("#FFFFFF"), Color.decode("#EBEBEB"));
        gradient.setView(note);


        notePane.setBorder(null);
        changeBackground(notePane);
        notePane.setViewport(gradient);
        app.add(notePane);

        changeBackground(notePane.getVerticalScrollBar());
        notePane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        notePane.getVerticalScrollBar().setPreferredSize(new Dimension(12, notePane.getHeight()));
        notePane.getVerticalScrollBar().setMinimumSize(new Dimension(12, notePane.getHeight()));
        notePane.getVerticalScrollBar().setMaximumSize(new Dimension(12, notePane.getHeight()));

        changeBackground(notePane.getHorizontalScrollBar());
        notePane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
        notePane.getHorizontalScrollBar().setPreferredSize(new Dimension(notePane.getWidth(), 12));
        notePane.getHorizontalScrollBar().setMinimumSize(new Dimension(notePane.getWidth(), 12));
        notePane.getHorizontalScrollBar().setMaximumSize(new Dimension(notePane.getWidth(), 12));


        changeBackground(bottomPane);
        bottomPane.setLayout(new BorderLayout());
        bottomPane.add(new JLabel(getImage("SizeGrip.png")), BorderLayout.EAST);
        app.add(bottomPane, BorderLayout.SOUTH);


        app.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                deleteNoteDialog();
                System.gc();
            }
        });
        app.addComponentListener(new ComponentAdapter() {
            public void componentMoved(ComponentEvent e) {
                if (app.isVisible()) {
                    Point location = app.getLocationOnScreen();
                    topBorder.setLocation(location.x - 4, location.y - 4);
                    leftBorder.setLocation(location.x - 5, location.y);
                    rightBorder.setLocation(location.x + app.getWidth(), location.y);
                    bottomBorder.setLocation(location.x - 5, location.y + app.getHeight());
                }
            }
            public void componentResized(ComponentEvent e) {
                deleteBut.setBounds(app.getWidth() - 26, 5, 18, 18);
                settingBut.setBounds(app.getWidth() - 44, 4, 18, 18);
                noteContext.setVisible(false);

                int maxWidth = (settingBut.getLocation().x - app.getWidth() / 2) * 2 - 10, width = (int) titleLabel.getPreferredSize().getWidth();
                int maxX = addBut.getLocation().x + 40;
                if (width > maxWidth) titleLabel.setBounds(maxX, 2, maxWidth, 22);
                else titleLabel.setBounds((app.getWidth() - width) / 2, 2, width, 22);

                if (app.isVisible()) {
                    Point location = app.getLocationOnScreen();
                    topBorder.setBounds(location.x - 4, location.y - 4, app.getWidth() + 8, 4);
                    leftBorder.setBounds(location.x - 5, location.y, 5, app.getHeight());
                    rightBorder.setBounds(location.x + app.getWidth(), location.y, 5, app.getHeight());
                    bottomBorder.setBounds(location.x - 5, location.y + app.getHeight(), app.getWidth() + 10, 10);
                    note.requestFocusInWindow();
                }
            }
        });
        app.addWindowFocusListener(new WindowFocusListener() {
            public void windowGainedFocus(WindowEvent e) {
                addBut.setVisible(true);
                deleteBut.setVisible(true);
                settingBut.setVisible(true);
                note.requestFocus();
            }
            public void windowLostFocus(WindowEvent e) {
                addBut.setVisible(false);
                deleteBut.setVisible(false);
                settingBut.setVisible(false);
            }
        });
    }

    void show() {
        titleLabel.setName("TitleName");
        titleLabel.setText(app.getTitle());
        titleLabel.setFont(new Font("Microsoft Yahei UI", Font.PLAIN, 12));
        titleLabel.setForeground(Color.GRAY);
        titleTooltip = createToolTip(titleLabel, app.getTitle());
        titleLabel.addMouseListener(titleTooltip);
        for (MouseListener m : titleBar.getMouseListeners()) titleLabel.addMouseListener(m);
        titleLabel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (buttonNum == 1) {
                    GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    PointerInfo pointer = MouseInfo.getPointerInfo();
                    if (pointer.getLocation().y >= environment.getMaximumWindowBounds().height - 2) {
                        for (GraphicsDevice device : environment.getScreenDevices()) {
                            try {
                                Robot r = new Robot(device);
                                r.mouseMove(pointer.getLocation().x, environment.getMaximumWindowBounds().height - 2);
                                app.setLocation(e.getXOnScreen() - winX, app.getLocation().y);
                            } catch (Exception exception) { exception.printStackTrace(); }
                        }
                    } else app.setLocation(e.getXOnScreen() - winX - titleLabel.getX(), e.getYOnScreen() - winY - 2);

                    Point location = app.getLocationOnScreen();
                    topBorder.setLocation(location.x - 5, location.y - 4);
                    leftBorder.setLocation(location.x - 5, location.y);
                    rightBorder.setLocation(location.x + app.getWidth(), location.y);
                    bottomBorder.setLocation(location.x - 5, location.y + app.getHeight());
                }
            }
        });
        int maxWidth = (settingBut.getLocation().x - app.getWidth() / 2) * 2 - 10, width = (int) titleLabel.getPreferredSize().getWidth();
        int maxX = addBut.getLocation().x + 40;
        if (width > maxWidth) titleLabel.setBounds(maxX, 2, maxWidth, 22);
        else titleLabel.setBounds((app.getWidth() - width) / 2, 2, width, 22);

        note.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
        note.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "undo");
        note.getActionMap().put("undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) undoManager.undo();
            }
        });
        note.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "redo");
        note.getActionMap().put("redo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) undoManager.redo();
            }
        });

        fadeEffect(app, true);
        Resizer resizer = new Resizer();
        resizer.resizeBottomSE((JLabel) bottomPane.getComponent(0));
        note.requestFocus();
        System.gc();
    }

    static void updateSettings() {
        try {
            File file = new File(dir + "Settings.txt");
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(noteCount + "\n");
            bufferedWriter.write(language + "\n");
            bufferedWriter.write(isShowOnClose + "\n");
            bufferedWriter.write(String.valueOf(isAnimate));
            bufferedWriter.close();
        } catch (Exception exception) { exception.printStackTrace(); }
    }

    static void saveNotes() {
        updateSettings();
        try {
            int i = 0;
            for (StickyNotes stickyNote : notes.keySet()) {
                if (notes.get(stickyNote)) {
                    i++;
                    File file = new File("src/savedNotes/" + i + ".txt");
                    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));

                    if (stickyNote.NOTE_COLOR == Color.YELLOW) bufferedWriter.write("Yellow");
                    else if (stickyNote.NOTE_COLOR == Color.BLUE) bufferedWriter.write("Blue");
                    else if (stickyNote.NOTE_COLOR == Color.GREEN) bufferedWriter.write("Green");
                    else if (stickyNote.NOTE_COLOR == Color.PINK) bufferedWriter.write("Pink");
                    else if (stickyNote.NOTE_COLOR == Color.MAGENTA) bufferedWriter.write("Purple");
                    else bufferedWriter.write("White");

                    bufferedWriter.write("\n" + stickyNote.app.getTitle());
                    bufferedWriter.write("\n" + stickyNote.app.getLocation().x + " " + stickyNote.app.getLocation().y
                            + " " +stickyNote.app.getWidth() + " " + stickyNote.app.getHeight() + "\n");
                    bufferedWriter.write(stickyNote.app.isAlwaysOnTop() + "\n");
                    Font font = stickyNote.note.getFont();
                    bufferedWriter.write(font.getName() + "," + font.getStyle() + "," + font.getSize());
                    bufferedWriter.write("\n" + stickyNote.note.getText());
                    bufferedWriter.close();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        System.gc();
    }

    void deleteNoteDialog() {
        changeButtonIcon(deleteBut, "");

        if (isShowOnClose) {
            JDialog dialog = new JDialog(app, "Blocking Dialog", true);
            dialog.setTitle("Sticky Notes");
            dialog.setSize(401, 155);
            dialog.setResizable(false);
            dialog.setAlwaysOnTop(true);
            dialog.setLocationRelativeTo(app);
            dialog.setLayout(null);

            JCheckBox show = new JCheckBox("  Don't display this message again");
            show.setFocusPainted(false);
            show.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            show.setBounds(9, 89, 210, 15);
            dialog.add(show);

            JButton yesBut = new JButton("Yes");
            yesBut.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            yesBut.setFocusPainted(false);
            yesBut.addActionListener(e -> {
                dialog.dispose();
                deleteNote();
            });
            yesBut.setBounds(233, 85, 68, 23);
            dialog.add(yesBut);

            JButton noBut = new JButton("No");
            noBut.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            noBut.setBounds(307, 85, 68, 23);
            noBut.setFocusPainted(false);
            noBut.addActionListener(e -> dialog.dispose());
            dialog.getRootPane().setDefaultButton(noBut);
            noBut.requestFocus();
            dialog.add(noBut);

            yesBut.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) dialog.dispose();
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        noBut.setFocusPainted(true);
                        noBut.requestFocus();
                        dialog.getRootPane().setDefaultButton(noBut);
                    }
                }
            });
            noBut.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) dialog.dispose();
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        yesBut.setFocusPainted(true);
                        yesBut.requestFocus();
                        dialog.getRootPane().setDefaultButton(yesBut);
                    }
                }
            });

            JLabel background = new JLabel(getImage("DeleteNote.png"));
            background.setBounds(0, 0, 385, 117);
            dialog.add(background);

            show.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) dialog.dispose();
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        yesBut.setFocusPainted(true);
                        yesBut.requestFocus();
                        dialog.getRootPane().setDefaultButton(yesBut);
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        noBut.setFocusPainted(true);
                        noBut.requestFocus();
                        dialog.getRootPane().setDefaultButton(noBut);
                    }
                }
            });
            show.addChangeListener(e -> {
                isShowOnClose = !show.isSelected();
                updateSettings();
            });

            dialog.setVisible(true);
        } else deleteNote();
        System.gc();
    }

    void deleteNote() {
        noteCount--;
        notes.replace(this, false);
        updateSettings();

        topBorder.setVisible(false);
        leftBorder.setVisible(false);
        rightBorder.setVisible(false);
        bottomBorder.setVisible(false);
        fadeEffect(app, false);

        if (noteCount == 0) {
            try { Thread.sleep(300); }
            catch (Exception e) { e.printStackTrace(); }
            System.exit(0);
        }
        System.gc();
    }

    void settingDialog() {
        JWindow border = new JWindow();
        border.setSize(370, 360);
        border.setAlwaysOnTop(true);

        Point location = settingBut.getLocationOnScreen();
        if (location.x <= screenResolution.width - 321) {
            if (location.y <= screenResolution.height - 344)
                border.setLocation(app.getLocation().x + app.getWidth() - 65, app.getLocation().y + 19);
            else
                border.setLocation(app.getLocation().x + app.getWidth() - 65, app.getLocation().y - 341);
        }
        else {
            if (location.y <= screenResolution.height - 344)
                border.setLocation(app.getLocation().x + app.getWidth() - 435, app.getLocation().y + 19);
            else
                border.setLocation(app.getLocation().x + app.getWidth() - 435, app.getLocation().y - 341);
        }
        border.setBackground(new Color(0, 0, 0, 0));
        border.setLayout(null);
        JLabel shadow = new JLabel(new ImageIcon("src/images/DialogShadow.png"));
        shadow.setBounds(0, 0, 370, 360);
        border.add(shadow);

        JDialog settingDialog = new JDialog();
        settingDialog.setUndecorated(true);
        settingDialog.setAlwaysOnTop(true);
        settingDialog.setBounds(border.getLocation().x + 22, border.getLocation().y + 19, 316, 306);
        settingDialog.setShape(new RoundRectangle2D.Double(0, 0, 316, 306, 15, 15));
        settingDialog.setContentPane(new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient;
                if (NOTE_COLOR == Color.YELLOW)
                    gradient = new GradientPaint(0, 0, Color.decode("#FDFDCA"), 0, getHeight(), Color.decode("#FCF9A3"));
                else if (NOTE_COLOR == Color.BLUE)
                    gradient = new GradientPaint(0, 0, Color.decode("#D9F3FB"), 0, getHeight(), Color.decode("#B8DBF4"));
                else if (NOTE_COLOR == Color.GREEN)
                    gradient = new GradientPaint(0, 0, Color.decode("#D2FFCC"), 0, getHeight(), Color.decode("#B1E8AE"));
                else if (NOTE_COLOR == Color.PINK)
                    gradient = new GradientPaint(0, 0, Color.decode("#F6D3F6"), 0, getHeight(), Color.decode("#EBAEEB"));
                else if (NOTE_COLOR == Color.MAGENTA)
                    gradient = new GradientPaint(0, 0, Color.decode("#DDD9FE"), 0, getHeight(), Color.decode("#C6B8FE"));
                else
                    gradient = new GradientPaint(0, 0, Color.decode("#FFFFFF"), 0, getHeight(), Color.decode("#EBEBEB"));

                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        });
        settingDialog.setLayout(null);


        JLabel title = new JLabel("Settings");
        title.setForeground(Color.GRAY);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setBounds(12, 7, 200, 30);
        settingDialog.add(title);

        discardBut.setName("DiscardBut");
        discardBut.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                if (isEntered) {
                    fadeEffect(border, false);
                    fadeEffect(settingDialog, false);
                }
                System.gc();
            }
        });
        discardBut.addMouseListener(createToolTip(discardBut, "Discard Edits"));
        discardBut.setBounds(284, 13, 20, 20);
        settingDialog.add(discardBut);


        JLabel noteTitleLabel = new JLabel("Title: ");
        noteTitleLabel.setBounds(13, 46, 50, 22);
        settingDialog.add(noteTitleLabel);

        JTextField noteTitle = new JTextField(app.getTitle());
        noteTitle.setBounds(94, 47, 208, 22);
        settingDialog.add(noteTitle);


        JLabel colorLabel = new JLabel("Note Color: ");
        colorLabel.setBounds(13, 71, 80, 22);
        settingDialog.add(colorLabel);

        JLabel colorPreview = new JLabel();
        colorPreview.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        colorPreview.setBounds(280, 74, 22, 21);
        settingDialog.add(colorPreview);

        String[] colors = {"Blue", "Green", "Pink", "Purple", "White", "Yellow"};
        JComboBox<String> color = new JComboBox<>(colors);
        for (int i = 0; i < colors.length; i++) {
            if (NOTE_COLOR == Color.BLUE) color.setSelectedIndex(0);
            else if (NOTE_COLOR == Color.GREEN) color.setSelectedIndex(1);
            else if (NOTE_COLOR == Color.PINK) color.setSelectedIndex(2);
            else if (NOTE_COLOR == Color.MAGENTA) color.setSelectedIndex(3);
            else if (NOTE_COLOR == Color.WHITE) color.setSelectedIndex(4);
            else color.setSelectedIndex(5);
        }
        colorPreview.setIcon(getImage(colors[color.getSelectedIndex()] + "NoteIcon.png"));
        color.setFocusable(false);
        color.setOpaque(false);
        color.addItemListener(e -> colorPreview.setIcon(getImage(colors[color.getSelectedIndex()] + "NoteIcon.png")));
        color.setBounds(94, 73, 180, 22);
        settingDialog.add(color);



        JLabel nameLabel = new JLabel("Font Name: ");
        nameLabel.setBounds(13, 97, 80, 22);
        settingDialog.add(nameLabel);

        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        JComboBox<String> fontName = new JComboBox<>(names);
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(note.getFont().getName())) {
                fontName.setSelectedIndex(i);
                break;
            }
        }
        fontName.setFocusable(false);
        fontName.setBounds(94, 99, 208, 22);
        settingDialog.add(fontName);


        JLabel styleLabel = new JLabel("Font Style: ");
        styleLabel.setBounds(13, 123, 80, 22);
        settingDialog.add(styleLabel);

        JComboBox<String> fontStyle = new JComboBox<>(new String[] {"Regular", "Bold", "Italic", "Bold Italic"});
        fontStyle.setSelectedIndex(note.getFont().getStyle());
        fontStyle.setFocusable(false);
        fontStyle.setBounds(94, 125, 95, 22);
        settingDialog.add(fontStyle);


        JLabel sizeLabel = new JLabel("Font Size: ");
        sizeLabel.setBounds(200, 124, 54, 22);
        settingDialog.add(sizeLabel);

        JSpinner fontSize = new JSpinner();
        fontSize.setValue(note.getFont().getSize());
        fontSize.setFocusable(false);
        fontSize.setBounds(256, 125, 46, 22);
        settingDialog.add(fontSize);


        JLabel languageLabel = new JLabel("Language: ");
        languageLabel.setBounds(13, 149, 120, 22);
        settingDialog.add(languageLabel);

        JComboBox<String> selectedLanguage = new JComboBox<>(new String[] {"English", "简体中文"});
        if (StickyNotes.language.equals("Chinese")) selectedLanguage.setSelectedIndex(1);
        else selectedLanguage.setSelectedIndex(0);
        selectedLanguage.setFocusable(false);
        selectedLanguage.setOpaque(false);
        selectedLanguage.setBounds(94, 151, 208, 22);
        settingDialog.add(selectedLanguage);


        JCheckBox isShow = new JCheckBox(" Show warning dialog on deletion");
        isShow.setFocusPainted(false);
        isShow.setOpaque(false);
        isShow.setSelected(isShowOnClose);
        isShow.setBounds(9, 178, 300, 23);
        settingDialog.add(isShow);

        JCheckBox isOnTop = new JCheckBox(" Make this note always on top");
        isOnTop.setFocusPainted(false);
        isOnTop.setOpaque(false);
        isOnTop.setSelected(app.isAlwaysOnTop());
        isOnTop.setBounds(9, 200, 300, 23);
        settingDialog.add(isOnTop);

        JCheckBox checkAnimate = new JCheckBox(" Display animations");
        checkAnimate.setFocusPainted(false);
        checkAnimate.setOpaque(false);
        checkAnimate.setSelected(isAnimate);
        checkAnimate.setBounds(9, 222, 300, 23);
        settingDialog.add(checkAnimate);


        JLabel helpLabel = new JLabel("<HTML><U>Help contribute to this project</U></HTML>");
        helpLabel.setForeground(Color.decode("#0078D7"));
        helpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        helpLabel.setBounds(13, 244, 160, 23);
        settingDialog.add(helpLabel);


        JButton saveBut = new JButton("Save changes");
        saveBut.setFocusPainted(false);
        saveBut.setOpaque(false);
        settingDialog.getRootPane().setDefaultButton(saveBut);
        saveBut.setBounds(116, 272, 106, 25);
        saveBut.addActionListener(e -> {
            isShowOnClose = isShow.isSelected();
            isAnimate = checkAnimate.isSelected();
            language = (String) selectedLanguage.getSelectedItem();
            app.setTitle(noteTitle.getText());
            app.setAlwaysOnTop(isOnTop.isSelected());

            titleLabel.setText(noteTitle.getText());
            titleLabel.removeMouseListener(titleTooltip);
            titleTooltip = createToolTip(titleLabel, noteTitle.getText());
            titleLabel.addMouseListener(titleTooltip);
            int maxWidth = (settingBut.getLocation().x - app.getWidth() / 2) * 2 - 10, width = (int) titleLabel.getPreferredSize().getWidth();
            int maxX = addBut.getLocation().x + 40;
            if (width > maxWidth) titleLabel.setBounds(maxX, 2, maxWidth, 22);
            else titleLabel.setBounds((app.getWidth() - width) / 2, 2, width, 22);

            int index = color.getSelectedIndex();
            if (index == 0) {
                NOTE_COLOR = Color.BLUE;
                titleBar.setBackground(Color.decode("#C9ECF8"));
                deleteBut.setIcon(getImage("DeleteButBlue.png"));
                GradientViewport gradient = new GradientViewport(Color.decode("#D9F3FB"), Color.decode("#B8DBF4"));
                gradient.setView(note);
                notePane.getVerticalScrollBar().setBackground(Color.decode("#B8DBF4"));
                notePane.getHorizontalScrollBar().setBackground(Color.decode("#B8DBF4"));
                notePane.setBackground(Color.decode("#B8DBF4"));
                notePane.setViewport(gradient);
                bottomPane.setBackground(Color.decode("#B8DBF4"));
            } else if (index == 1) {
                NOTE_COLOR = Color.GREEN;
                titleBar.setBackground(Color.decode("#C5F7C1"));
                deleteBut.setIcon(getImage("DeleteButGreen.png"));
                GradientViewport gradient = new GradientViewport(Color.decode("#D2FFCC"), Color.decode("#B1E8AE"));
                gradient.setView(note);
                notePane.setBackground(Color.decode("#B1E8AE"));
                notePane.getVerticalScrollBar().setBackground(Color.decode("#B1E8AE"));
                notePane.getHorizontalScrollBar().setBackground(Color.decode("#B1E8AE"));
                notePane.setViewport(gradient);
                bottomPane.setBackground(Color.decode("#B1E8AE"));
            } else if (index == 2) {
                NOTE_COLOR = Color.PINK;
                titleBar.setBackground(Color.decode("#F1C3F1"));
                deleteBut.setIcon(getImage("DeleteButPink.png"));
                GradientViewport gradient = new GradientViewport(Color.decode("#F6D3F6"), Color.decode("#EBAEEB"));
                gradient.setView(note);
                notePane.setBackground(Color.decode("#EBAEEB"));
                notePane.getVerticalScrollBar().setBackground(Color.decode("#EBAEEB"));
                notePane.getHorizontalScrollBar().setBackground(Color.decode("#EBAEEB"));
                notePane.setViewport(gradient);
                bottomPane.setBackground(Color.decode("#EBAEEB"));
            } else if (index == 3) {
                NOTE_COLOR = Color.MAGENTA;
                titleBar.setBackground(Color.decode("#D4CDF3"));
                deleteBut.setIcon(getImage("DeleteButPurple.png"));
                GradientViewport gradient = new GradientViewport(Color.decode("#DDD9FE"), Color.decode("#C6B8FE"));
                gradient.setView(note);
                notePane.setBackground(Color.decode("#C6B8FE"));
                notePane.getVerticalScrollBar().setBackground(Color.decode("#C6B8FE"));
                notePane.getHorizontalScrollBar().setBackground(Color.decode("#C6B8FE"));
                notePane.setViewport(gradient);
                bottomPane.setBackground(Color.decode("#C6B8FE"));
            } else if (index == 4) {
                NOTE_COLOR = Color.WHITE;
                titleBar.setBackground(Color.decode("#F5F5F5"));
                deleteBut.setIcon(getImage("DeleteButWhite.png"));
                GradientViewport gradient = new GradientViewport(Color.decode("#FFFFFF"), Color.decode("#EBEBEB"));
                gradient.setView(note);
                notePane.setBackground(Color.decode("#EBEBEB"));
                notePane.getVerticalScrollBar().setBackground(Color.decode("#EBEBEB"));
                notePane.getHorizontalScrollBar().setBackground(Color.decode("#EBEBEB"));
                notePane.setViewport(gradient);
                bottomPane.setBackground(Color.decode("#EBEBEB"));
            } else {
                NOTE_COLOR = Color.YELLOW;
                titleBar.setBackground(Color.decode("#F8F7B6"));
                deleteBut.setIcon(getImage("DeleteButYellow.png"));
                GradientViewport gradient = new GradientViewport(Color.decode("#FDFDCA"), Color.decode("#FCF9A3"));
                gradient.setView(note);
                notePane.setBackground(Color.decode("#FCF9A3"));
                notePane.getVerticalScrollBar().setBackground(Color.decode("#FCF9A3"));
                notePane.getHorizontalScrollBar().setBackground(Color.decode("#FCF9A3"));
                notePane.setViewport(gradient);
                bottomPane.setBackground(Color.decode("#FCF9A3"));
            }

            int style;
            if (fontStyle.getSelectedIndex() == 0) style = Font.PLAIN;
            else if (fontStyle.getSelectedIndex() == 1) style = Font.BOLD;
            else if (fontStyle.getSelectedIndex() == 2) style = Font.ITALIC;
            else style = Font.BOLD + Font.ITALIC;
            try { note.setFont(new Font((String) fontName.getSelectedItem(), style, (Integer) fontSize.getValue())); }
            catch (Exception exception) { exception.printStackTrace(); }

            changeAppIcon();
            updateSettings();
            saveNotes();
            fadeEffect(border, false);
            fadeEffect(settingDialog, false);
            System.gc();
        });
        settingDialog.add(saveBut);

        JButton cancelBut = new JButton("Cancel");
        cancelBut.setFocusPainted(false);
        cancelBut.setOpaque(false);
        cancelBut.setBounds(229, 272, 72, 25);
        cancelBut.addActionListener(e -> {
            fadeEffect(border, false);
            fadeEffect(settingDialog, false);
            System.gc();
        });
        settingDialog.add(cancelBut);

        for (int i = 1; i < settingDialog.getContentPane().getComponentCount(); i++)
            settingDialog.getContentPane().getComponent(i).setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noteTitle.setFont(new Font("Microsoft Yahei UI", Font.PLAIN, 12));
        selectedLanguage.setFont(new Font("Microsoft Yahei UI", Font.PLAIN, 12));

        settingDialog.addWindowFocusListener(new WindowAdapter() {
            public void windowLostFocus(WindowEvent e) {
                fadeEffect(border, false);
                fadeEffect(settingDialog, false);
                System.gc();
            }
        });

        fadeEffect(border, true);
        fadeEffect(settingDialog, true);
        System.gc();
    }

    static ImageIcon getImage(String name) {
        if (name.contains("AddBut")) return new ImageIcon("src/addButtonIcons/" + name);
        else if (name.contains("DeleteBut")) return new ImageIcon("src/deleteButtonIcons/" + name);
        else if (name.contains("Shadow")) return new ImageIcon("src/windowShadows/" + name);
        return new ImageIcon("src/images/" + name);
    }

    static void fadeEffect(Component frame, boolean state) {
        if (isAnimate) {
            int time = 300, FPS = 33;
            float DELTA = FPS / (float) time;

            if (frame.getClass() == JFrame.class) ((JFrame) frame).setOpacity(state ? 0f : 1f);
            else if (frame.getClass() == JDialog.class) ((JDialog) frame).setOpacity(state ? 0f : 1f);
            else ((JWindow) frame).setOpacity(state ? 0f : 1f);

            if (state) frame.setVisible(true);
            Timer timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                float opacity = state ? 0f : 1f;
                final float delta = state ? DELTA : -DELTA;

                public void run() {
                    opacity += delta;
                    if (opacity < 0) {
                        frame.setVisible(false);
                        if (frame.getClass() == JFrame.class) ((JFrame) frame).setOpacity(1.0f);
                        else if (frame.getClass() == JDialog.class) ((JDialog) frame).setOpacity(1.0f);
                        else ((JWindow) frame).setOpacity(1.0f);
                        timer.cancel();
                    } else if (opacity > 1) {
                        if (frame.getClass() == JFrame.class) ((JFrame) frame).setOpacity(1.0f);
                        else if (frame.getClass() == JDialog.class) ((JDialog) frame).setOpacity(1.0f);
                        else ((JWindow) frame).setOpacity(1.0f);
                        timer.cancel();
                    } else {
                        if (frame.getClass() == JFrame.class) ((JFrame) frame).setOpacity(opacity);
                        else if (frame.getClass() == JDialog.class) ((JDialog) frame).setOpacity(opacity);
                        else ((JWindow) frame).setOpacity(opacity);
                    }
                }
            };
            timer.scheduleAtFixedRate(timerTask, FPS, FPS);
        } else frame.setVisible(state);
        System.gc();
    }

    MouseListener createToolTip(JLabel button, String tooltipText) {
        return new MouseAdapter() {
            final JWindow tooltip = new JWindow();
            Timer timer;

            public void mouseEntered(MouseEvent e) {
                isEntered = true;
                if (!isResizing) {
                    if (!app.isFocused() && !button.getName().equals("DiscardBut")) {
                        addBut.setVisible(true);
                        deleteBut.setVisible(true);
                        settingBut.setVisible(true);
                    }

                    JLabel text = new JLabel(tooltipText);
                    text.setFont(new Font("Microsoft Yahei UI", Font.PLAIN, 12));
                    int w = text.getPreferredSize().width, h = text.getPreferredSize().height;
                    text.setBounds(12, (30 - h) / 2, w, h);

                    tooltip.setLayout(null);
                    tooltip.setSize(w + 24, 30);
                    tooltip.setShape(new RoundRectangle2D.Double(0, 0, w + 24, 30, 7, 7));
                    tooltip.setAlwaysOnTop(true);
                    tooltip.add(text);

                    JLabel leftBorder = new JLabel(getImage("LeftTooltip.png"));
                    leftBorder.setBounds(0, 0, 4, 30);
                    tooltip.add(leftBorder);
                    JLabel centerBorder = new JLabel(getImage("CentreTooltip.png"));
                    centerBorder.setBounds(4, 0, tooltip.getWidth() - 8, 30);
                    tooltip.add(centerBorder);
                    JLabel rightBorder = new JLabel(getImage("RightTooltip.png"));
                    rightBorder.setBounds(tooltip.getWidth() - 4, 0, 4, 30);
                    tooltip.add(rightBorder);

                    Point location = MouseInfo.getPointerInfo().getLocation();
                    if (location.y + 48 > screenResolution.height) tooltip.setLocation(location.x, location.y - 48);
                    else tooltip.setLocation(location.x, location.y + 18);

                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        public void run() {
                            int maxWidth = (settingBut.getLocation().x - app.getWidth() / 2) * 2 - 10;
                            int width = (int) titleLabel.getPreferredSize().getWidth();
                            if ((button.getName().equals("TitleName"))) {
                                if (width > maxWidth && !isPressed) fadeEffect(tooltip, true);
                            } else {
                                if (!isPressed) fadeEffect(tooltip, true);
                            }
                        }
                    }, 600);
                    timer.schedule(new TimerTask() {
                        public void run() {
                            int maxWidth = (settingBut.getLocation().x - app.getWidth() / 2) * 2 - 10;
                            int width = (int) titleLabel.getPreferredSize().getWidth();
                            if ((button.getName().equals("TitleName"))) {
                                if (width > maxWidth && !isPressed) fadeEffect(tooltip, false);
                            } else {
                                if (!isPressed) fadeEffect(tooltip, false);
                            }
                        }
                    }, 5600);

                    switch (button.getName()) {
                        case "AddBut", "DeleteBut":
                            if (!isPressed) changeButtonIcon(button, "Entered");
                            else changeButtonIcon(button, "Clicked");
                            break;
                        case "SettingBut":
                            settingBut.setIcon(getImage("SettingButEntered.png"));
                            break;
                        case "DiscardBut":
                            discardBut.setIcon(getImage("DiscardButEntered.png"));
                            break;
                    }
                }
            }
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                if (!isResizing) {
                    timer.cancel();
                    fadeEffect(tooltip, false);
                    noteContext.setVisible(false);

                    switch (button.getName()) {
                        case "AddBut", "DeleteBut" -> changeButtonIcon(button, "Clicked");
                        case "SettingBut" -> settingBut.setIcon(getImage("SettingButEntered.png"));
                        case "DiscardBut" -> discardBut.setIcon(getImage("DiscardButClicked.png"));
                    }
                }
            }
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                if (!isResizing) {
                    if (isEntered) {
                        switch (button.getName()) {
                            case "AddBut" -> new StickyNotes(NOTE_COLOR).show();
                            case "DeleteBut" -> deleteNoteDialog();
                            case "SettingBut" -> settingDialog();
                        }
                    }
                }
            }
            public void mouseExited(MouseEvent e) {
                isEntered = false;
                if (!isResizing) {
                    timer.cancel();
                    fadeEffect(tooltip, false);

                    if (!app.isFocused()) {
                        addBut.setVisible(false);
                        deleteBut.setVisible(false);
                        settingBut.setVisible(false);
                    }

                    if (!button.getName().equals("TitleName")) changeButtonIcon(button, "");
                    addBut.setIcon(getImage("AddBut.png"));
                    settingBut.setIcon(getImage("SettingBut.png"));
                    discardBut.setIcon(getImage("DiscardBut.png"));
                }
            }
        };
    }

    void changeBackground(JComponent component) {
        if (NOTE_COLOR == Color.YELLOW) component.setBackground(Color.decode("#FCF9A3"));
        else if (NOTE_COLOR == Color.BLUE) component.setBackground(Color.decode("#B8DBF4"));
        else if (NOTE_COLOR == Color.GREEN) component.setBackground(Color.decode("#B1E8AE"));
        else if (NOTE_COLOR == Color.PINK) component.setBackground(Color.decode("#EBAEEB"));
        else if (NOTE_COLOR == Color.MAGENTA) component.setBackground(Color.decode("#C6B8FE"));
        else component.setBackground(Color.decode("#EBEBEB"));
    }

    void changeButtonIcon(JLabel button, String status) {
        if (NOTE_COLOR == Color.YELLOW) button.setIcon(getImage(button.getName() + "Yellow" + status + ".png"));
        else if (NOTE_COLOR == Color.BLUE) button.setIcon(getImage(button.getName() + "Blue" + status + ".png"));
        else if (NOTE_COLOR == Color.GREEN) button.setIcon(getImage(button.getName() + "Green" + status + ".png"));
        else if (NOTE_COLOR == Color.PINK) button.setIcon(getImage(button.getName() + "Pink" + status + ".png"));
        else if (NOTE_COLOR == Color.MAGENTA) button.setIcon(getImage(button.getName() + "Purple" + status + ".png"));
        else button.setIcon(getImage(button.getName() + "White" + status + ".png"));
    }

    void changeAppIcon() {
        if (NOTE_COLOR == Color.YELLOW) app.setIconImage(getImage("YellowLogo.png").getImage());
        else if (NOTE_COLOR == Color.BLUE) app.setIconImage(getImage("BlueLogo.png").getImage());
        else if (NOTE_COLOR == Color.GREEN) app.setIconImage(getImage("GreenLogo.png").getImage());
        else if (NOTE_COLOR == Color.PINK) app.setIconImage(getImage("PinkLogo.png").getImage());
        else if (NOTE_COLOR == Color.MAGENTA) app.setIconImage(getImage("PurpleLogo.png").getImage());
        else app.setIconImage(getImage("WhiteLogo.png").getImage());
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        File file = new File(dir + "Settings.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        int count = Integer.parseInt(bufferedReader.readLine());
        language = bufferedReader.readLine();
        isShowOnClose = bufferedReader.readLine().equals("true");
        isAnimate = bufferedReader.readLine().equals("true");
        bufferedReader.close();

        for (File f : Objects.requireNonNull(new File("src/savedNotes").listFiles())) {
            if (Integer.parseInt(f.getName().substring(0, 1)) > count) Files.delete(f.toPath());
        }

        File[] files = new File("src/savedNotes").listFiles();
        assert files != null;
        Arrays.sort(files);

        if (count == 0) new StickyNotes(Color.YELLOW).show();
        else {
            for (int i = 0; i < count; i++) {
                StickyNotes note;
                bufferedReader = new BufferedReader(new FileReader(files[i]));

                String color = bufferedReader.readLine();
                note = switch (color) {
                    case "Yellow" -> new StickyNotes(Color.YELLOW);
                    case "Blue" -> new StickyNotes(Color.BLUE);
                    case "Green" -> new StickyNotes(Color.GREEN);
                    case "Pink" -> new StickyNotes(Color.PINK);
                    case "Purple" -> new StickyNotes(Color.MAGENTA);
                    default -> new StickyNotes(Color.WHITE);
                };

                note.app.setTitle(bufferedReader.readLine());
                String[] bound = bufferedReader.readLine().split(" ");
                note.app.setBounds(Integer.parseInt(bound[0]), Integer.parseInt(bound[1]), Integer.parseInt(bound[2]), Integer.parseInt(bound[3]));
                note.app.setAlwaysOnTop(bufferedReader.readLine().equals("true"));

                String[] font = bufferedReader.readLine().split(",");
                note.note.setFont(new Font(font[0], Integer.parseInt(font[1]), Integer.parseInt(font[2])));
                note.note.setText(bufferedReader.lines().collect(Collectors.joining("\n")));
                note.show();

                bufferedReader.close();
            }
        }
    }
}
