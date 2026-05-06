/**
 * ChessModel represents the data and rules of the chess game.
 * It stores the state of the chessboard, pieces, whose turn it is,
 * and contains the logic for validating moves, checking for check/checkmate, etc.
 * 
 * Note: This class must remain independent of any GUI components (no Swing imports).
 */
public class ChessModel {

    public enum PlayerColor {
        WHITE, BLACK
    }

    public enum PieceType {
        PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
    }

    public static class Piece {
        private final PieceType type;
        private final PlayerColor color;
        private boolean hasMoved; // Tracks if the piece has moved (useful for pawns, castling)

        public Piece(PieceType type, PlayerColor color) {
            this.type = type;
            this.color = color;
            this.hasMoved = false;
        }

        public PieceType getType() { return type; }
        public PlayerColor getColor() { return color; }
        public boolean hasMoved() { return hasMoved; }
        public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }
        
        @Override
        public String toString() {
            return color + "_" + type;
        }
    }

    // Helper class to keep track of moves (crucial for en passant)
    public static class Move {
        public final int startRow, startCol, endRow, endCol;
        public final Piece movedPiece;

        public Move(int startRow, int startCol, int endRow, int endCol, Piece movedPiece) {
            this.startRow = startRow;
            this.startCol = startCol;
            this.endRow = endRow;
            this.endCol = endCol;
            this.movedPiece = movedPiece;
        }
    }

    // 8x8 grid representing the board
    // board[row][col] where row 0 is the top (Black's starting side)
    // and row 7 is the bottom (White's starting side)
    private Piece[][] board;
    private PlayerColor currentTurn;
    private Move lastMove; // Track the most recent move made on the board
    private boolean isGameOver;

    public ChessModel() {
        board = new Piece[8][8];
        currentTurn = PlayerColor.WHITE;
        lastMove = null;
        isGameOver = false;
        initializeBoard();
    }

    /**
     * Initializes the chessboard with pieces in their standard starting positions.
     */
    private void initializeBoard() {
        // Initialize Black pieces (Top of the board, row 0 and 1)
        board[0][0] = new Piece(PieceType.ROOK, PlayerColor.BLACK);
        board[0][1] = new Piece(PieceType.KNIGHT, PlayerColor.BLACK);
        board[0][2] = new Piece(PieceType.BISHOP, PlayerColor.BLACK);
        board[0][3] = new Piece(PieceType.QUEEN, PlayerColor.BLACK);
        board[0][4] = new Piece(PieceType.KING, PlayerColor.BLACK);
        board[0][5] = new Piece(PieceType.BISHOP, PlayerColor.BLACK);
        board[0][6] = new Piece(PieceType.KNIGHT, PlayerColor.BLACK);
        board[0][7] = new Piece(PieceType.ROOK, PlayerColor.BLACK);
        for (int i = 0; i < 8; i++) {
            board[1][i] = new Piece(PieceType.PAWN, PlayerColor.BLACK);
        }

        // Initialize White pieces (Bottom of the board, row 6 and 7)
        for (int i = 0; i < 8; i++) {
            board[6][i] = new Piece(PieceType.PAWN, PlayerColor.WHITE);
        }
        board[7][0] = new Piece(PieceType.ROOK, PlayerColor.WHITE);
        board[7][1] = new Piece(PieceType.KNIGHT, PlayerColor.WHITE);
        board[7][2] = new Piece(PieceType.BISHOP, PlayerColor.WHITE);
        board[7][3] = new Piece(PieceType.QUEEN, PlayerColor.WHITE);
        board[7][4] = new Piece(PieceType.KING, PlayerColor.WHITE);
        board[7][5] = new Piece(PieceType.BISHOP, PlayerColor.WHITE);
        board[7][6] = new Piece(PieceType.KNIGHT, PlayerColor.WHITE);
        board[7][7] = new Piece(PieceType.ROOK, PlayerColor.WHITE);
    }

    public Piece getPiece(int row, int col) {
        if (row < 0 || row >= 8 || col < 0 || col >= 8) {
            return null;
        }
        return board[row][col];
    }

    public PlayerColor getCurrentTurn() {
        return currentTurn;
    }

    public void switchTurn() {
        currentTurn = (currentTurn == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
    }

    /**
     * Checks if a move is valid based on the rules for the specific piece.
     */
    public boolean isValidMove(int startRow, int startCol, int endRow, int endCol) {
        // Bounds checking
        if (startRow < 0 || startRow >= 8 || startCol < 0 || startCol >= 8 ||
            endRow < 0 || endRow >= 8 || endCol < 0 || endCol >= 8) {
            return false;
        }

        Piece piece = board[startRow][startCol];
        
        // Cannot move an empty square, and must move piece of the current turn
        if (piece == null || piece.getColor() != currentTurn) {
            return false;
        }

        // Cannot move to a square occupied by your own piece
        Piece targetPiece = board[endRow][endCol];
        if (targetPiece != null && targetPiece.getColor() == piece.getColor()) {
            return false;
        }

        int rowDiff = endRow - startRow;
        int colDiff = endCol - startCol;

        switch (piece.getType()) {
            case PAWN:
                int dir = (piece.getColor() == PlayerColor.WHITE) ? -1 : 1;
                
                // Move straight forward
                if (colDiff == 0) {
                    // 1 square forward
                    if (rowDiff == dir && targetPiece == null) {
                        return true;
                    }
                    // 2 squares forward
                    if (rowDiff == 2 * dir && targetPiece == null && 
                        board[startRow + dir][startCol] == null && !piece.hasMoved()) {
                        return true;
                    }
                }
                // Capture diagonally
                else if (Math.abs(colDiff) == 1 && rowDiff == dir) {
                    // Standard capture
                    if (targetPiece != null && targetPiece.getColor() != piece.getColor()) {
                        return true;
                    }
                    // En Passant capture
                    if (targetPiece == null && lastMove != null) {
                        if (lastMove.movedPiece.getType() == PieceType.PAWN &&
                            Math.abs(lastMove.endRow - lastMove.startRow) == 2 &&
                            lastMove.endRow == startRow &&
                            lastMove.endCol == endCol) {
                            return true;
                        }
                    }
                }
                return false;

            case ROOK:
                return (rowDiff == 0 || colDiff == 0) && isPathClear(startRow, startCol, endRow, endCol);

            case KNIGHT:
                return (Math.abs(rowDiff) == 2 && Math.abs(colDiff) == 1) ||
                       (Math.abs(rowDiff) == 1 && Math.abs(colDiff) == 2);

            case BISHOP:
                return Math.abs(rowDiff) == Math.abs(colDiff) && isPathClear(startRow, startCol, endRow, endCol);

            case QUEEN:
                return (rowDiff == 0 || colDiff == 0 || Math.abs(rowDiff) == Math.abs(colDiff)) && 
                       isPathClear(startRow, startCol, endRow, endCol);

            case KING:
                // Basic 1-square move. Castling not implemented yet.
                return Math.abs(rowDiff) <= 1 && Math.abs(colDiff) <= 1;

            default:
                return false;
        }
    }

    /**
     * Helper method to verify if the path is clear for sliding pieces (Rook, Bishop, Queen).
     */
    private boolean isPathClear(int startRow, int startCol, int endRow, int endCol) {
        int rowStep = Integer.compare(endRow, startRow);
        int colStep = Integer.compare(endCol, startCol);
        
        int r = startRow + rowStep;
        int c = startCol + colStep;
        
        while (r != endRow || c != endCol) {
            if (board[r][c] != null) {
                return false;
            }
            r += rowStep;
            c += colStep;
        }
        return true;
    }

    /**
     * Executes the move if valid, handles captures, and switches turn.
     * Returns true if the move was successfully made.
     */
    public boolean movePiece(int startRow, int startCol, int endRow, int endCol) {
        if (!isValidMove(startRow, startCol, endRow, endCol)) {
            return false;
        }

        Piece piece = board[startRow][startCol];
        
        // Handle En Passant capture removal
        if (piece.getType() == PieceType.PAWN && Math.abs(startCol - endCol) == 1 && board[endRow][endCol] == null) {
            board[startRow][endCol] = null; // Remove the captured pawn which is on the start row
        }

        // Apply move
        board[endRow][endCol] = piece;
        board[startRow][startCol] = null;
        piece.setHasMoved(true);

        // Record this move for en passant validation next turn
        lastMove = new Move(startRow, startCol, endRow, endCol, piece);

        // Advance turn
        switchTurn();
        return true;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean isGameOver) {
        this.isGameOver = isGameOver;
    }
}
