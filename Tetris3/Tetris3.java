import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class Tetris3 extends JPanel implements ActionListener {

    public static final int BOARD_WIDTH = 10;
    public static final int BOARD_HEIGHT = 20;
    public static final int CELL_SIZE = 30;

    private Timer timer;
    private boolean isFallingFinished = false;
    private boolean isStarted = false;
    private boolean isPaused = false;
    private int numLinesRemoved = 0;
    private int curX = 0;
    private int curY = 0;
    private Shape curPiece;
    private Tetrominoes[] board;

    private int score = 0;
    private int level = 1;

    private Shape nextPiece;
    private Shape holdPiece = null;
    private boolean holdUsed = false;

    enum Tetrominoes {
        NoShape, ZShape, SShape, LineShape, TShape, SquareShape, LShape, MirroredLShape
    }

    class Shape {
        private Tetrominoes pieceShape;
        private int coords[][];
        private int[][][] coordsTable;

        public Shape() {
            coords = new int[4][2];
            setShape(Tetrominoes.NoShape);
        }

        public void setShape(Tetrominoes shape) {
            coordsTable = new int[][][]{
                    {{0, 0}, {0, 0}, {0, 0}, {0, 0}},
                    {{0, -1}, {0, 0}, {-1, 0}, {-1, 1}},
                    {{0, -1}, {0, 0}, {1, 0}, {1, 1}},
                    {{0, -1}, {0, 0}, {0, 1}, {0, 2}},
                    {{-1, 0}, {0, 0}, {1, 0}, {0, 1}},
                    {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
                    {{-1, -1}, {0, -1}, {0, 0}, {0, 1}},
                    {{1, -1}, {0, -1}, {0, 0}, {0, 1}}
            };
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

        public int x(int index) {
            return coords[index][0];
        }

        public int y(int index) {
            return coords[index][1];
        }

        public Tetrominoes getShape() {
            return pieceShape;
        }

        public void setX(int index, int x) {
            coords[index][0] = x;
        }

        public void setY(int index, int y) {
            coords[index][1] = y;
        }

        public int minX() {
            int m = coords[0][0];
            for (int i = 1; i < 4; i++) {
                m = Math.min(m, coords[i][0]);
            }
            return m;
        }

        public int minY() {
            int m = coords[0][1];
            for (int i = 1; i < 4; i++) {
                m = Math.min(m, coords[i][1]);
            }
            return m;
        }

        public Shape rotateRight() {
            if (pieceShape == Tetrominoes.SquareShape)
                return this;

            Shape result = new Shape();
            result.pieceShape = pieceShape;

            for (int i = 0; i < 4; ++i) {
                result.setX(i, -y(i));
                result.setY(i, x(i));
            }
            return result;
        }

        // コピー用
        public Shape clone() {
            Shape s = new Shape();
            s.setShape(pieceShape);
            for (int i = 0; i < 4; i++) {
                s.setX(i, coords[i][0]);
                s.setY(i, coords[i][1]);
            }
            return s;
        }
    }

    public Tetris3() {
        setFocusable(true);
        curPiece = new Shape();
        nextPiece = new Shape();
        nextPiece.setRandomShape();
        board = new Tetrominoes[BOARD_WIDTH * BOARD_HEIGHT];
        clearBoard();
        timer = new Timer(400, this);
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

    private int squareWidth() {
        return CELL_SIZE;
    }

    private int squareHeight() {
        return CELL_SIZE;
    }

    private Tetrominoes shapeAt(int x, int y) {
        return board[(y * BOARD_WIDTH) + x];
    }

    public void start() {
        if (isPaused)
            return;

        isStarted = true;
        isFallingFinished = false;
        numLinesRemoved = 0;
        score = 0;
        level = 1;
        holdPiece = null;
        holdUsed = false;
        clearBoard();
        newPiece();
        timer.setDelay(calcTimerDelay());
        timer.start();
    }

    private void pause() {
        if (!isStarted)
            return;

        isPaused = !isPaused;

        if (isPaused) {
            timer.stop();
            setToolTipText("paused");
        } else {
            timer.start();
            setToolTipText("");
        }
        repaint();
    }

    public void paint(Graphics g) {
        super.paint(g);

        Dimension size = getSize();
        int boardTop = (int) size.getHeight() - BOARD_HEIGHT * squareHeight();

        // ブロック描画
        for (int i = 0; i < BOARD_HEIGHT; ++i) {
            for (int j = 0; j < BOARD_WIDTH; ++j) {
                Tetrominoes shape = shapeAt(j, BOARD_HEIGHT - i - 1);
                if (shape != Tetrominoes.NoShape)
                    drawSquare(g, j * squareWidth(), boardTop + i * squareHeight(), shape);
            }
        }

        // 現在の落下中のピース描画
        if (curPiece.getShape() != Tetrominoes.NoShape) {
            for (int i = 0; i < 4; ++i) {
                int x = curX + curPiece.x(i);
                int y = curY - curPiece.y(i);
                drawSquare(g, x * squareWidth(), boardTop + (BOARD_HEIGHT - y - 1) * squareHeight(), curPiece.getShape());
            }
        }

        // スコアとレベルを描画
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Score: " + score, 10, 25);
        g.drawString("Level: " + level, 10, 45);

        // NEXTピース描画（右上、小さく）
        int nextX = BOARD_WIDTH * CELL_SIZE + 20;
        int nextY = 20;
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("NEXT", nextX, nextY - 5);
        drawSmallPiece(g, nextPiece, nextX, nextY);

        // HOLDピース描画（NEXTの左、小さく）
        int holdX = nextX - 100;
        int holdY = nextY;
        g.drawString("HOLD", holdX, holdY - 5);
        if (holdPiece != null) {
            drawSmallPiece(g, holdPiece, holdX, holdY);
        }

        if (!isStarted) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(Color.RED);
            g.drawString("Game Over", size.width / 2 - 80, size.height / 2);
        }

        if (isPaused) {
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.setColor(Color.BLUE);
            g.drawString("Paused", size.width / 2 - 50, size.height / 2);
        }
    }

    private void drawSmallPiece(Graphics g, Shape piece, int x, int y) {
        int size = CELL_SIZE / 2;
        for (int i = 0; i < 4; i++) {
            int px = x + (piece.x(i) - piece.minX()) * size;
            int py = y + (piece.y(i) - piece.minY()) * size;
            drawSmallSquare(g, px, py, piece.getShape(), size);
        }
    }

    private void drawSmallSquare(Graphics g, int x, int y, Tetrominoes shape, int size) {
        Color colors[] = {
                Color.BLACK, Color.RED, Color.GREEN, Color.CYAN,
                Color.MAGENTA, Color.ORANGE, Color.BLUE, Color.YELLOW
        };

        Color color = colors[shape.ordinal()];

        g.setColor(color);
        g.fillRect(x + 1, y + 1, size - 2, size - 2);

        g.setColor(color.brighter());
        g.drawLine(x, y + size - 1, x, y);
        g.drawLine(x, y, x + size - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + size - 1, x + size - 1, y + size - 1);
        g.drawLine(x + size - 1, y + size - 1, x + size - 1, y + 1);
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

    private void clearBoard() {
        for (int i = 0; i < BOARD_HEIGHT * BOARD_WIDTH; ++i)
            board[i] = Tetrominoes.NoShape;
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

    private void newPiece() {
        curPiece = nextPiece.clone();
        nextPiece.setRandomShape();
        curX = BOARD_WIDTH / 2 + 1;
        curY = BOARD_HEIGHT - 1 + curPiece.minY();

        holdUsed = false; // 新しいピースが来たらホールドは使えるようにする

        if (!tryMove(curPiece, curX, curY)) {
            curPiece.setShape(Tetrominoes.NoShape);
            timer.stop();
            isStarted = false;
            repaint();
        }
        repaint();
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
            switch (numFullLines) {
                case 1:
                    score += 40 * level;
                    break;
                case 2:
                    score += 100 * level;
                    break;
                case 3:
                    score += 300 * level;
                    break;
                case 4:
                    score += 1200 * level;
                    break;
            }
            updateLevel();
            isFallingFinished = true;
            curPiece.setShape(Tetrominoes.NoShape);
            repaint();
        }
    }

    private void updateLevel() {
        int newLevel = numLinesRemoved / 10 + 1;
        if (newLevel > level) {
            level = newLevel;
            timer.setDelay(calcTimerDelay());
        }
    }

    private int calcTimerDelay() {
        int delay = 400 - (level - 1) * 35;
        return Math.max(delay, 100);
    }

    // ホールド機能
    private void hold() {
        if (holdUsed) return; // 1ターンに1回のみ

        if (holdPiece == null) {
            holdPiece = curPiece.clone();
            newPiece();
        } else {
            Shape temp = holdPiece.clone();
            holdPiece = curPiece.clone();
            curPiece = temp;
            curX = BOARD_WIDTH / 2 + 1;
            curY = BOARD_HEIGHT - 1 + curPiece.minY();

            if (!tryMove(curPiece, curX, curY)) {
                // 動けなかったらゲームオーバー
                timer.stop();
                isStarted = false;
            }
            repaint();
        }
        holdUsed = true;
    }

    private void drawSquare(Graphics g, int x, int y, Tetrominoes shape) {
        Color colors[] = {
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

    class TAdapter extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (!isStarted || curPiece.getShape() == Tetrominoes.NoShape)
                return;

            int keycode = e.getKeyCode();

            if (keycode == KeyEvent.VK_P) {
                pause();
                return;
            }

            if (isPaused)
                return;

            switch (keycode) {
                case KeyEvent.VK_LEFT:
                    tryMove(curPiece, curX - 1, curY);
                    break;
                case KeyEvent.VK_RIGHT:
                    tryMove(curPiece, curX + 1, curY);
                    break;
                case KeyEvent.VK_DOWN:
                    oneLineDown();
                    break;
                case KeyEvent.VK_UP:
                    tryMove(curPiece.rotateRight(), curX, curY);
                    break;
                case KeyEvent.VK_SPACE:
                    dropDown();
                    break;
                case KeyEvent.VK_Z:
                    hold();
                    break;
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Tetris3 with Next and Hold");
        Tetris3 game = new Tetris3();

        frame.add(game);
        frame.setSize(BOARD_WIDTH * CELL_SIZE + 150, BOARD_HEIGHT * CELL_SIZE + 50);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
