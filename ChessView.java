import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * ChessView is responsible for rendering the chess game visually.
 * It extends JPanel to provide a custom drawing area for the chessboard and pieces.
 * It strictly reads from the model and never mutates its state.
 */
public class ChessView extends JPanel {

    private final ChessModel model;

    // Classic chess board colors
    private static final Color LIGHT_SQUARE = new Color(215, 220, 190);
    private static final Color DARK_SQUARE = new Color(118, 150, 86);

    // UI state that does not belong in the model
    private int selectedRow = -1;
    private int selectedCol = -1;

    public static class Arrow {
        int startRow, startCol, endRow, endCol;
        public Arrow(int startRow, int startCol, int endRow, int endCol) {
            this.startRow = startRow;
            this.startCol = startCol;
            this.endRow = endRow;
            this.endCol = endCol;
        }
    }
    
    private final List<Arrow> arrows = new ArrayList<>();
    private Arrow inProgressArrow = null;

    public ChessView(ChessModel model) {
        this.model = model;
    }

    public void setSelectedSquare(int row, int col) {
        this.selectedRow = row;
        this.selectedCol = col;
    }

    public void addArrow(int startRow, int startCol, int endRow, int endCol) {
        arrows.add(new Arrow(startRow, startCol, endRow, endCol));
    }

    public void setInProgressArrow(int startRow, int startCol, int endRow, int endCol) {
        if (startRow == -1) {
            inProgressArrow = null;
        } else {
            inProgressArrow = new Arrow(startRow, startCol, endRow, endCol);
        }
    }

    public void clearArrows() {
        arrows.clear();
        inProgressArrow = null;
    }

    /**
     * Returns the [row, col] on the board for a given pixel coordinate, 
     * or null if the click was outside the 8x8 grid.
     */
    public int[] getSquareFromCoordinates(int x, int y) {
        int width = getWidth();
        int height = getHeight();
        int leftPanelWidth = 200;
        int rightPanelWidth = 200;
        int availableWidth = Math.max(0, width - leftPanelWidth - rightPanelWidth);
        int boardSize = Math.min(availableWidth, height);
        int squareSize = boardSize / 8;

        int offsetX = leftPanelWidth + (availableWidth - boardSize) / 2;
        int offsetY = (height - boardSize) / 2;

        if (x < offsetX || x >= offsetX + boardSize || y < offsetY || y >= offsetY + boardSize) {
            return null; // Clicked outside the board
        }

        int col = (x - offsetX) / squareSize;
        int row = (y - offsetY) / squareSize;
        return new int[]{row, col};
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (model == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        // Enable anti-aliasing for smoother piece rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int leftPanelWidth = 200;
        int rightPanelWidth = 200;
        int availableWidth = Math.max(0, width - leftPanelWidth - rightPanelWidth);

        // Calculate the size of each square to fit the remaining panel while maintaining a square aspect ratio
        int boardSize = Math.min(availableWidth, height);
        int squareSize = boardSize / 8;

        // Center the board in the remaining space
        int offsetX = leftPanelWidth + (availableWidth - boardSize) / 2;
        int offsetY = (height - boardSize) / 2;
        
        // Draw Captured Pieces Panel aligned with the left edge of the board
        drawCapturedPieces(g2d, offsetX - leftPanelWidth, offsetY, leftPanelWidth, boardSize);

        // Draw Move History Panel aligned with the right edge of the board
        drawMoveHistory(g2d, offsetX + boardSize, offsetY, rightPanelWidth, boardSize);

        // Draw the 8x8 board and pieces
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // Calculate square color
                boolean isLightSquare = (row + col) % 2 == 0;
                g2d.setColor(isLightSquare ? LIGHT_SQUARE : DARK_SQUARE);
                
                int x = offsetX + col * squareSize;
                int y = offsetY + row * squareSize;
                g2d.fillRect(x, y, squareSize, squareSize);

                // Highlight selected square
                if (row == selectedRow && col == selectedCol) {
                    g2d.setColor(new Color(255, 255, 0, 100)); // Yellow with transparency
                    g2d.fillRect(x, y, squareSize, squareSize);
                }

                // Draw board coordinates
                g2d.setFont(new Font("SansSerif", Font.BOLD, squareSize / 5));
                FontMetrics fmCoords = g2d.getFontMetrics();
                int padding = Math.max(2, squareSize / 20);

                // Draw numbers (ranks) on the first column (col == 0)
                if (col == 0) {
                    String rankText = String.valueOf(8 - row);
                    g2d.setColor(isLightSquare ? DARK_SQUARE : LIGHT_SQUARE);
                    int textX = x + padding;
                    int textY = y + fmCoords.getAscent() + padding - 2; // -2 for visual centering adjustment
                    g2d.drawString(rankText, textX, textY);
                }

                // Draw letters (files) on the last row (row == 7)
                if (row == 7) {
                    String fileText = String.valueOf((char) ('a' + col));
                    g2d.setColor(isLightSquare ? DARK_SQUARE : LIGHT_SQUARE);
                    int textX = x + squareSize - fmCoords.stringWidth(fileText) - padding;
                    int textY = y + squareSize - fmCoords.getDescent() - padding + 2;
                    g2d.drawString(fileText, textX, textY);
                }

                // Read piece from model and draw it
                ChessModel.Piece piece = model.getPiece(row, col);
                
                // Highlight King in check
                if (piece != null && piece.getType() == ChessModel.PieceType.KING && model.isInCheck(piece.getColor())) {
                    g2d.setColor(new Color(255, 0, 0, 150)); // Semi-transparent red
                    g2d.fillRect(x, y, squareSize, squareSize);
                }
                
                if (piece != null) {
                    drawPiece(g2d, piece, x, y, squareSize);
                }

                // Draw valid move indicators
                if (selectedRow != -1 && selectedCol != -1) {
                    if (model.isValidMove(selectedRow, selectedCol, row, col)) {
                        g2d.setColor(new Color(0, 0, 0, 60)); // Semi-transparent grey
                        if (piece == null) {
                            // Empty square -> small solid circle
                            int circleRadius = squareSize / 6;
                            int centerX = x + squareSize / 2;
                            int centerY = y + squareSize / 2;
                            g2d.fillOval(centerX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);
                        } else {
                            // Capture -> hollow ring
                            int ringRadius = squareSize * 3 / 8; // slightly smaller than square half
                            int centerX = x + squareSize / 2;
                            int centerY = y + squareSize / 2;
                            g2d.setStroke(new java.awt.BasicStroke(squareSize / 12f));
                            g2d.drawOval(centerX - ringRadius, centerY - ringRadius, ringRadius * 2, ringRadius * 2);
                            g2d.setStroke(new java.awt.BasicStroke()); // reset stroke
                        }
                    }
                }
            }
        }

        // Draw committed arrows
        for (Arrow arrow : arrows) {
            drawArrow(g2d, arrow, squareSize, offsetX, offsetY);
        }

        // Draw in-progress drag arrow
        if (inProgressArrow != null) {
            drawArrow(g2d, inProgressArrow, squareSize, offsetX, offsetY);
        }

        // Draw Game Over message if the game is over
        if (model.isGameOver()) {
            drawGameOver(g2d, width, height);
        }
    }

    private void drawArrow(Graphics2D g2d, Arrow arrow, int squareSize, int offsetX, int offsetY) {
        g2d.setColor(new Color(255, 170, 0, 180)); // Orange-ish with transparency
        
        int startX = offsetX + arrow.startCol * squareSize + squareSize / 2;
        int startY = offsetY + arrow.startRow * squareSize + squareSize / 2;
        int endX = offsetX + arrow.endCol * squareSize + squareSize / 2;
        int endY = offsetY + arrow.endRow * squareSize + squareSize / 2;

        // If dragging in the same square, don't draw an arrow
        if (startX == endX && startY == endY) return;

        double angle = Math.atan2(endY - startY, endX - startX);
        
        int arrowHeadLength = squareSize / 3;
        
        // Draw main line using a thick stroke
        g2d.setStroke(new java.awt.BasicStroke(squareSize / 8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        
        // Shorten the line so it doesn't poke through the arrowhead
        int lineEndX = endX - (int) (Math.cos(angle) * arrowHeadLength);
        int lineEndY = endY - (int) (Math.sin(angle) * arrowHeadLength);
        g2d.drawLine(startX, startY, lineEndX, lineEndY);

        // Draw arrowhead (Polygon)
        int[] xPoints = {
            endX,
            endX - (int) (Math.cos(angle - Math.PI / 6) * arrowHeadLength),
            endX - (int) (Math.cos(angle + Math.PI / 6) * arrowHeadLength)
        };
        int[] yPoints = {
            endY,
            endY - (int) (Math.sin(angle - Math.PI / 6) * arrowHeadLength),
            endY - (int) (Math.sin(angle + Math.PI / 6) * arrowHeadLength)
        };
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.setStroke(new java.awt.BasicStroke()); // Reset stroke
    }

    private void drawPiece(Graphics2D g2d, ChessModel.Piece piece, int x, int y, int size) {
        String pieceSymbol = getPieceSymbol(piece);
        
        // Use solid unicode pieces and set the draw color to White or Black.
        g2d.setColor(piece.getColor() == ChessModel.PlayerColor.WHITE ? Color.WHITE : Color.BLACK);
        
        // Font size is slightly smaller than the square size
        g2d.setFont(new Font("SansSerif", Font.PLAIN, size * 4 / 5));
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(pieceSymbol);
        
        // Center the text horizontally and vertically in the square
        int textX = x + (size - textWidth) / 2;
        int textY = y + (size + fm.getAscent() - fm.getDescent()) / 2;
        
        g2d.drawString(pieceSymbol, textX, textY);
    }

    /**
     * Maps piece types to their solid Unicode character equivalents.
     */
    private String getPieceSymbol(ChessModel.Piece piece) {
        switch (piece.getType()) {
            case KING: return "\u265A";
            case QUEEN: return "\u265B";
            case ROOK: return "\u265C";
            case BISHOP: return "\u265D";
            case KNIGHT: return "\u265E";
            case PAWN: return "\u265F";
            default: return "";
        }
    }

    private void drawCapturedPieces(Graphics2D g2d, int panelX, int panelY, int panelWidth, int panelHeight) {
        // Draw background for the panel
        g2d.setColor(new Color(130, 130, 130)); // Lighter grey panel background
        g2d.fillRect(panelX, panelY, panelWidth, panelHeight);

        List<ChessModel.Piece> capturedWhite = new ArrayList<>(model.getCapturedWhitePieces());
        List<ChessModel.Piece> capturedBlack = new ArrayList<>(model.getCapturedBlackPieces());

        // Sort: Queen(highest enum) -> Pawn(lowest enum)
        capturedWhite.sort((p1, p2) -> p2.getType().compareTo(p1.getType()));
        capturedBlack.sort((p1, p2) -> p2.getType().compareTo(p1.getType()));

        int pieceSize = 36; // Fixed size for captured pieces
        
        // Draw White captured pieces
        int startX = panelX + 15;
        int startY = panelY + 60;
        int x = startX;
        int y = startY;

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.drawString("White Pieces", x, y - 30);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, pieceSize));

        for (ChessModel.Piece p : capturedWhite) {
            String symbol = getPieceSymbol(p);
            g2d.drawString(symbol, x, y);
            x += pieceSize - 5; // Slight overlap for neat grouping
            if (x + pieceSize > panelX + panelWidth) {
                x = startX;
                y += pieceSize + 5;
            }
        }

        // Draw Black captured pieces
        x = startX;
        y = panelY + panelHeight / 2 + 60;

        g2d.setColor(Color.WHITE); // label is white
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.drawString("Black Pieces", x, y - 30);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, pieceSize));
        g2d.setColor(Color.BLACK); // pieces are black
        
        for (ChessModel.Piece p : capturedBlack) {
            String symbol = getPieceSymbol(p);
            g2d.drawString(symbol, x, y);
            x += pieceSize - 5;
            if (x + pieceSize > panelX + panelWidth) {
                x = startX;
                y += pieceSize + 5;
            }
        }
    }

    private void drawMoveHistory(Graphics2D g2d, int panelX, int panelY, int panelWidth, int panelHeight) {
        // Draw background for the panel
        g2d.setColor(new Color(130, 130, 130)); // Match captured pieces panel color
        g2d.fillRect(panelX, panelY, panelWidth, panelHeight);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2d.drawString("Move History", panelX + 15, panelY + 30);

        List<String> history = model.getMoveHistory();
        if (history == null || history.isEmpty()) return;

        int fontSize = 14;
        g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        
        int rowHeight = fontSize + 10;
        int maxRows = (panelHeight - 60) / rowHeight; // Leave room for title and padding
        
        // Calculate how many turns we have (1 turn = 1 white move + 1 black move)
        int totalTurns = (history.size() + 1) / 2;
        
        // Determine starting turn to draw (scroll logic)
        int startTurn = Math.max(0, totalTurns - maxRows);

        int drawY = panelY + 60;
        
        for (int turn = startTurn; turn < totalTurns; turn++) {
            int whiteMoveIndex = turn * 2;
            int blackMoveIndex = turn * 2 + 1;

            String turnNumber = (turn + 1) + ".";
            String whiteMove = history.get(whiteMoveIndex);
            String blackMove = (blackMoveIndex < history.size()) ? history.get(blackMoveIndex) : "";

            // Draw Turn Number
            g2d.setColor(new Color(220, 220, 220)); // Lighter text for numbers
            g2d.drawString(turnNumber, panelX + 10, drawY);

            // Draw White Move
            g2d.setColor(Color.WHITE);
            g2d.drawString(whiteMove, panelX + 40, drawY);

            // Draw Black Move
            g2d.setColor(Color.BLACK);
            g2d.drawString(blackMove, panelX + 120, drawY);

            drawY += rowHeight;
        }
    }

    private void drawGameOver(Graphics2D g2d, int width, int height) {
        String msg = "GAME OVER";
        g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        int msgWidth = fm.stringWidth(msg);
        int msgHeight = fm.getHeight();
        
        // Semi-transparent dark background for the banner
        g2d.setColor(new Color(0, 0, 0, 180));
        int padding = 20;
        int bannerWidth = msgWidth + padding * 2;
        int bannerHeight = msgHeight + padding * 2;
        int bannerX = (width - bannerWidth) / 2;
        int bannerY = (height - bannerHeight) / 2;
        g2d.fillRect(bannerX, bannerY, bannerWidth, bannerHeight);
        
        // White text
        g2d.setColor(Color.WHITE);
        g2d.drawString(msg, bannerX + padding, bannerY + fm.getAscent() + padding);
    }
}
