import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class Tetris4 extends JPanel implements ActionListener {

    public static final int BOARD_WIDTH = 10;
    public static final int BOARD_HEIGHT = 20;
    public static final int CELL_SIZE = 30;

    private Timer timer;
    private boolean isFallingFinished = false;
    private boolean isStarted = false;
    private boolean isPaused = false;
    private boolean holdUsed = false;

    private int numLinesRemoved = 0;
    private int curX = 0;
    private int curY = 0;

    private Shape curPiece;
    private Shape nextPiece;
    private Shape holdPiece;
    private Tetrominoes[] board;

    enum Tetrominoes {
        NoShape, ZShape, SShape, LineShape, TShape, SquareShape, LShape, MirroredLShape
    }

    class Shape {
        private Tetrominoes pieceShape;
        private int[][] coords;
        private final int[][][] coordsTable;

        public Shape() {
            coords = new int[4][2];
            coordsTable = new int[][][] {
                { {0,0}, {0,0}, {0,0}, {0,0} },
                { {0,-1}, {0,0}, {-1,0}, {-1,1} },
                { {0,-1}, {0,0}, {1,0}, {1,1} },
                { {0,-1}, {0,0}, {0,1}, {0,2} },
                { {-1,0}, {0,0}, {1,0}, {0,1} },
                { {0,0}, {1,0}, {0,1}, {1,1} },
                { {-1,-1}, {0,-1}, {0,0}, {0,1} },
                { {1,-1}, {0,-1}, {0,0}, {0,1} }
            };
            setShape(Tetrominoes.NoShape);
        }

        public void setShape(Tetrominoes shape) {
            for (int i = 0; i < 4; i++) {
                coords[i][0] = coordsTable[shape.ordinal()][i][0];
                coords[i][1] = coordsTable[shape.ordinal()][i][1];
            }
            pieceShape = shape;
        }

        public void setRandomShape() {
            Random r = new Random();
            int x = Math.abs(r.nextInt()) % 7 + 1;
            setShape(Tetrominoes.values()[x]);
        }

        public int x(int index) { return coords[index][0]; }
        public int y(int index) { return coords[index][1]; }
        public Tetrominoes getShape() { return pieceShape; }

        public int minY() {
            int m = coords[0][1];
            for (int i=1; i<4; i++) m = Math.min(m, coords[i][1]);
            return m;
        }

        public Shape rotateRight() {
            if (pieceShape == Tetrominoes.SquareShape)
                return this;
            Shape result = new Shape();
            result.pieceShape = pieceShape;
            for (int i = 0; i < 4; ++i) {
                result.coords[i][0] = -coords[i][1];
                result.coords[i][1] = coords[i][0];
            }
            return result;
        }
    }

    public Tetris4() {
        setFocusable(true);
        setBackground(Color.BLACK);
        curPiece = new Shape();
        nextPiece = new Shape();
        nextPiece.setRandomShape();
        holdPiece = new Shape();
        timer = new Timer(400, this);
        timer.start();
        board = new Tetrominoes[BOARD_WIDTH * BOARD_HEIGHT];
        clearBoard();
        addKeyListener(new TAdapter());
        start();
    }

    public void actionPerformed(ActionEvent e) {
        if (isFallingFinished) {
            isFallingFinished = false;
            newPiece();
        } else {
            oneLineDown();
        }
    }

    private void clearBoard() {
        for (int i = 0; i < BOARD_HEIGHT * BOARD_WIDTH; ++i)
            board[i] = Tetrominoes.NoShape;
    }

    private void newPiece() {
        curPiece = nextPiece;
        nextPiece = new Shape();
        nextPiece.setRandomShape();
        curX = BOARD_WIDTH / 2;
        curY = BOARD_HEIGHT - 1 + curPiece.minY();
        holdUsed = false;

        if (!tryMove(curPiece, curX, curY)) {
            curPiece.setShape(Tetrominoes.NoShape);
            timer.stop();
            isStarted = false;
            setToolTipText("Game Over");
        }
    }

    private boolean tryMove(Shape newPiece, int newX, int newY) {
        for (int i = 0; i < 4; ++i) {
            int x = newX + newPiece.x(i);
            int y = newY - newPiece.y(i);
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT)
                return false;
            if (shapeAt(x, y) != Tetrominoes.NoShape)
                return false;
        }
        curPiece = newPiece;
        curX = newX;
        curY = newY;
        repaint();
        return true;
    }

    private void dropDown() {
        int newY = curY;
        while (newY > 0) {
            if (!tryMove(curPiece, curX, newY - 1))
                break;
            --newY;
        }
        pieceDropped();
    }

    private void oneLineDown() {
        if (!tryMove(curPiece, curX, curY - 1))
            pieceDropped();
    }

    private void pieceDropped() {
        for (int i = 0; i < 4; ++i) {
            int x = curX + curPiece.x(i);
            int y = curY - curPiece.y(i);
            board[(y * BOARD_WIDTH) + x] = curPiece.getShape();
        }
        removeFullLines();
        if (!isFallingFinished)
            newPiece();
    }

    private void removeFullLines() {
        int numFullLines = 0;

        for (int i = BOARD_HEIGHT - 1; i >= 0; --i) {
            boolean lineIsFull = true;
            for (int j = 0; j < BOARD_WIDTH; ++j) {
                if (shapeAt(j, i) == Tetrominoes.NoShape) {
                    lineIsFull = false;
                    break;
                }
            }
            if (lineIsFull) {
                ++numFullLines;
                for (int k = i; k < BOARD_HEIGHT - 1; ++k) {
                    for (int j = 0; j < BOARD_WIDTH; ++j)
                        board[(k * BOARD_WIDTH) + j] = shapeAt(j, k + 1);
                }
            }
        }

        if (numFullLines > 0) {
            numLinesRemoved += numFullLines;
            isFallingFinished = true;
            curPiece.setShape(Tetrominoes.NoShape);
        }
    }

    private Tetrominoes shapeAt(int x, int y) {
        return board[(y * BOARD_WIDTH) + x];
    }

    private void drawSquare(Graphics g, int x, int y, Tetrominoes shape) {
        Color[] colors = {
            Color.BLACK, Color.RED, Color.GREEN, Color.CYAN,
            Color.MAGENTA, Color.ORANGE, Color.BLUE, Color.YELLOW
        };
        Color color = colors[shape.ordinal()];
        g.setColor(color);
        g.fillRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
        g.setColor(color.brighter());
        g.drawLine(x, y + CELL_SIZE - 1, x, y);
        g.drawLine(x, y, x + CELL_SIZE - 1, y);
        g.setColor(color.darker());
        g.drawLine(x + 1, y + CELL_SIZE - 1, x + CELL_SIZE - 1, y + CELL_SIZE - 1);
        g.drawLine(x + CELL_SIZE - 1, y + CELL_SIZE - 1, x + CELL_SIZE - 1, y + 1);
    }

    private void drawSmallSquare(Graphics g, int x, int y, Tetrominoes shape) {
        Color[] colors = {
            Color.BLACK, Color.RED, Color.GREEN, Color.CYAN,
            Color.MAGENTA, Color.ORANGE, Color.BLUE, Color.YELLOW
        };
        Color color = colors[shape.ordinal()];
        g.setColor(color);
        g.fillRect(x + 1, y + 1, 14, 14);
        g.setColor(color.brighter());
        g.drawLine(x, y + 15, x, y);
        g.drawLine(x, y, x + 15, y);
        g.setColor(color.darker());
        g.drawLine(x + 1, y + 14, x + 14, y + 14);
        g.drawLine(x + 14, y + 14, x + 14, y + 1);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.BLACK);

        int boardTop = getHeight() - BOARD_HEIGHT * CELL_SIZE;
        g.setColor(Color.GRAY);
        g.drawRect(0, boardTop, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);

        for (int i = 0; i < BOARD_HEIGHT; ++i) {
            for (int j = 0; j < BOARD_WIDTH; ++j) {
                Tetrominoes shape = shapeAt(j, BOARD_HEIGHT - i - 1);
                if (shape != Tetrominoes.NoShape)
                    drawSquare(g, j * CELL_SIZE, boardTop + i * CELL_SIZE, shape);
            }
        }

        if (curPiece.getShape() != Tetrominoes.NoShape) {
            for (int i = 0; i < 4; ++i) {
                int x = curX + curPiece.x(i);
                int y = curY - curPiece.y(i);
                drawSquare(g, x * CELL_SIZE, boardTop + (BOARD_HEIGHT - y - 1) * CELL_SIZE, curPiece.getShape());
            }
        }

        // HOLD
        g.setColor(Color.WHITE);
        g.drawString("HOLD", BOARD_WIDTH * CELL_SIZE + 20, 20);
        if (holdPiece.getShape() != Tetrominoes.NoShape) {
            // Y座標を20px下にずらす（元は30）
            for (int i = 0; i < 4; ++i) {
                int x = holdPiece.x(i);
                int y = holdPiece.y(i);
                drawSmallSquare(g, BOARD_WIDTH * CELL_SIZE + 20 + x * 15, 50 + y * 15, holdPiece.getShape()); // 30 → 50 に変更
            }
        }

        // NEXT
        g.setColor(Color.WHITE);
        g.drawString("NEXT", BOARD_WIDTH * CELL_SIZE + 120, 20);
        for (int i = 0; i < 4; ++i) {
            int x = nextPiece.x(i);
            int y = nextPiece.y(i);
            drawSmallSquare(g, BOARD_WIDTH * CELL_SIZE + 120 + x * 15, 50 + y * 15, nextPiece.getShape()); // 30 → 50 に変更
        }

        // スコア・レベル
        g.setColor(Color.WHITE);
        g.drawString("SCORE: " + numLinesRemoved, BOARD_WIDTH * CELL_SIZE + 20, 150);
        g.drawString("LEVEL: " + (1 + numLinesRemoved / 10), BOARD_WIDTH * CELL_SIZE + 20, 170);
    }

    public void start() {
        if (isPaused) return;
        isStarted = true;
        isFallingFinished = false;
        numLinesRemoved = 0;
        clearBoard();
        newPiece();
        timer.start();
    }

    private void pause() {
        if (!isStarted) return;
        isPaused = !isPaused;
        if (isPaused) {
            timer.stop();
            setToolTipText("Paused");
        } else {
            timer.start();
            setToolTipText("");
        }
        repaint();
    }

    class TAdapter extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (!isStarted || curPiece.getShape() == Tetrominoes.NoShape) return;
            int keycode = e.getKeyCode();

            if (keycode == KeyEvent.VK_P) {
                pause();
                return;
            }

            if (isPaused) return;

            switch (keycode) {
                case KeyEvent.VK_LEFT:
                    tryMove(curPiece, curX - 1, curY); break;
                case KeyEvent.VK_RIGHT:
                    tryMove(curPiece, curX + 1, curY); break;
                case KeyEvent.VK_DOWN:
                    oneLineDown(); break;
                case KeyEvent.VK_UP:
                    tryMove(curPiece.rotateRight(), curX, curY); break;
                case KeyEvent.VK_SPACE:
                    dropDown(); break;
                case KeyEvent.VK_Z:
                    if (!holdUsed) {
                        Shape temp = holdPiece;
                        holdPiece = curPiece;
                        if (temp.getShape() == Tetrominoes.NoShape) {
                            newPiece();
                        } else {
                            curPiece = temp;
                            curX = BOARD_WIDTH / 2;
                            curY = BOARD_HEIGHT - 1 + curPiece.minY();
                            tryMove(curPiece, curX, curY);
                        }
                        holdUsed = true;
                    }
                    break;
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tetris4 - Black Background");
        Tetris4 game = new Tetris4();
        frame.add(game);
        frame.setSize(500, 640);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
