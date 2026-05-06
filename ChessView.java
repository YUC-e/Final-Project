import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * ChessView is responsible for rendering the chess game visually.
 * It extends JPanel to provide a custom drawing area for the chessboard and pieces.
 * It strictly reads from the model and never mutates its state.
 */
public class ChessView extends JPanel {

    private final ChessModel model;

    // Classic chess board colors
    private static final Color LIGHT_SQUARE = new Color(238, 238, 210);
    private static final Color DARK_SQUARE = new Color(118, 150, 86);

    public ChessView(ChessModel model) {
        this.model = model;
        // The controller will add mouse listeners to this panel.
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
        // Calculate the size of each square to fit the panel while maintaining a square aspect ratio
        int boardSize = Math.min(width, height);
        int squareSize = boardSize / 8;

        // Center the board if the window is not perfectly square
        int offsetX = (width - boardSize) / 2;
        int offsetY = (height - boardSize) / 2;

        // Draw the 8x8 board and pieces
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                // Calculate square color
                boolean isLightSquare = (row + col) % 2 == 0;
                g2d.setColor(isLightSquare ? LIGHT_SQUARE : DARK_SQUARE);
                
                int x = offsetX + col * squareSize;
                int y = offsetY + row * squareSize;
                g2d.fillRect(x, y, squareSize, squareSize);

                // Read piece from model and draw it
                ChessModel.Piece piece = model.getPiece(row, col);
                if (piece != null) {
                    drawPiece(g2d, piece, x, y, squareSize);
                }
            }
        }

        // Draw Game Over message if the game is over
        if (model.isGameOver()) {
            drawGameOver(g2d, width, height);
        }
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
