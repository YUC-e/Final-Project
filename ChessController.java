import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * ChessController acts as the bridge between the Model and the View.
 * It listens for user actions from the View, updates the Model accordingly,
 * and instructs the View to refresh based on changes in the Model.
 */
public class ChessController {

    private ChessModel model;
    private ChessView view;

    private int selectedRow = -1;
    private int selectedCol = -1;
    
    private int rightClickStartRow = -1;
    private int rightClickStartCol = -1;

    public ChessController(ChessModel model, ChessView view) {
        this.model = model;
        this.view = view;
        
        setupMouseListeners();
    }

    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (model.isGameOver()) return;

                int[] square = view.getSquareFromCoordinates(e.getX(), e.getY());
                if (square == null) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        view.clearArrows();
                        view.repaint();
                    }
                    return;
                }

                int row = square[0];
                int col = square[1];

                if (SwingUtilities.isLeftMouseButton(e)) {
                    handleLeftClick(row, col);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    rightClickStartRow = row;
                    rightClickStartCol = col;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (model.isGameOver()) return;

                if (SwingUtilities.isRightMouseButton(e) && rightClickStartRow != -1) {
                    int[] square = view.getSquareFromCoordinates(e.getX(), e.getY());
                    if (square != null) {
                        view.setInProgressArrow(rightClickStartRow, rightClickStartCol, square[0], square[1]);
                        view.repaint();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (model.isGameOver()) return;

                if (SwingUtilities.isRightMouseButton(e)) {
                    int[] square = view.getSquareFromCoordinates(e.getX(), e.getY());
                    if (square != null && rightClickStartRow != -1) {
                        int row = square[0];
                        int col = square[1];
                        if (row != rightClickStartRow || col != rightClickStartCol) {
                            view.addArrow(rightClickStartRow, rightClickStartCol, row, col);
                        }
                    }
                    rightClickStartRow = -1;
                    rightClickStartCol = -1;
                    view.setInProgressArrow(-1, -1, -1, -1);
                    view.repaint();
                }
            }
        };

        view.addMouseListener(mouseAdapter);
        view.addMouseMotionListener(mouseAdapter);
    }

    private void handleLeftClick(int row, int col) {
        if (selectedRow != -1 && selectedCol != -1) {
            // Check if we click the same square to deselect
            if (selectedRow == row && selectedCol == col) {
                selectedRow = -1;
                selectedCol = -1;
                view.setSelectedSquare(-1, -1);
                view.repaint();
                return;
            }

            // Check for promotion
            ChessModel.Piece pieceToMove = model.getPiece(selectedRow, selectedCol);
            ChessModel.PieceType promotion = null;
            if (model.isValidMove(selectedRow, selectedCol, row, col)) {
                if (pieceToMove != null && pieceToMove.getType() == ChessModel.PieceType.PAWN && (row == 0 || row == 7)) {
                    ChessModel.PieceType[] options = {
                        ChessModel.PieceType.QUEEN, 
                        ChessModel.PieceType.ROOK, 
                        ChessModel.PieceType.BISHOP, 
                        ChessModel.PieceType.KNIGHT
                    };
                    int choice = javax.swing.JOptionPane.showOptionDialog(
                        view, 
                        "Choose a piece to promote to:", 
                        "Pawn Promotion", 
                        javax.swing.JOptionPane.DEFAULT_OPTION, 
                        javax.swing.JOptionPane.PLAIN_MESSAGE, 
                        null, 
                        options, 
                        options[0]
                    );
                    if (choice >= 0) {
                        promotion = options[choice];
                    } else {
                        // User cancelled
                        selectedRow = -1;
                        selectedCol = -1;
                        view.setSelectedSquare(-1, -1);
                        view.repaint();
                        return;
                    }
                }
            }

            // Try to move
            if (model.movePiece(selectedRow, selectedCol, row, col, promotion)) {
                // Successful move
                selectedRow = -1;
                selectedCol = -1;
                view.setSelectedSquare(-1, -1);
                view.clearArrows(); // Arrows removed after move
                view.repaint();
            } else {
                // Invalid move or selecting another piece of our color
                ChessModel.Piece piece = model.getPiece(row, col);
                if (piece != null && piece.getColor() == model.getCurrentTurn()) {
                    selectedRow = row;
                    selectedCol = col;
                    view.setSelectedSquare(row, col);
                    view.repaint();
                } else {
                    selectedRow = -1;
                    selectedCol = -1;
                    view.setSelectedSquare(-1, -1);
                    view.repaint();
                }
            }
        } else {
            ChessModel.Piece piece = model.getPiece(row, col);
            if (piece != null && piece.getColor() == model.getCurrentTurn()) {
                selectedRow = row;
                selectedCol = col;
                view.setSelectedSquare(row, col);
                view.repaint();
            } else {
                // Left click on empty square or enemy piece clears arrows
                view.clearArrows();
                view.repaint();
            }
        }
    }

    public static void main(String[] args) {
        // Ensure GUI creation is done on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // Instantiate the components
            ChessModel model = new ChessModel();
            ChessView view = new ChessView(model);
            ChessController controller = new ChessController(model, view);

            // Setup the main window (JFrame)
            JFrame frame = new JFrame("Chess Game");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Add the view (JPanel) to the frame
            frame.add(view);
            
            // Set an initial size so it opens a visible blank window
            frame.setPreferredSize(new Dimension(800, 800));
            frame.pack();
            frame.setLocationRelativeTo(null); // Center on screen
            frame.setVisible(true);
        });
    }
}
