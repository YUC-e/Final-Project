import java.util.ArrayList;
import java.util.List;

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
    private List<Piece> capturedWhitePieces;
    private List<Piece> capturedBlackPieces;
    private List<String> moveHistory;

    public ChessModel() {
        board = new Piece[8][8];
        currentTurn = PlayerColor.WHITE;
        lastMove = null;
        isGameOver = false;
        capturedWhitePieces = new ArrayList<>();
        capturedBlackPieces = new ArrayList<>();
        moveHistory = new ArrayList<>();
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
     * Checks if a move is completely valid (follows piece rules AND doesn't leave King in check).
     */
    public boolean isValidMove(int startRow, int startCol, int endRow, int endCol) {
        if (!isPseudoLegalMove(startRow, startCol, endRow, endCol)) {
            return false;
        }
        return !wouldMoveLeaveKingInCheck(startRow, startCol, endRow, endCol);
    }

    /**
     * Checks if a move follows the fundamental movement rules for the specific piece.
     * It does not consider if the move leaves the King in check.
     */
    private boolean isPseudoLegalMove(int startRow, int startCol, int endRow, int endCol) {
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
                // Basic 1-square move.
                if (Math.abs(rowDiff) <= 1 && Math.abs(colDiff) <= 1) {
                    return true;
                }
                
                // Castling
                if (!piece.hasMoved() && rowDiff == 0 && Math.abs(colDiff) == 2) {
                    // Cannot castle out of check
                    if (isInCheck(piece.getColor())) return false;

                    // Kingside castling
                    if (colDiff == 2) {
                        Piece rook = board[startRow][7];
                        if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                            // Check that the spaces between king and rook are empty
                            if (board[startRow][5] == null && board[startRow][6] == null) {
                                PlayerColor oppColor = (piece.getColor() == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
                                // Cannot castle through or into check
                                if (!isSquareAttacked(startRow, 5, oppColor) && !isSquareAttacked(startRow, 6, oppColor)) {
                                    return true;
                                }
                            }
                        }
                    }
                    // Queenside castling
                    else if (colDiff == -2) {
                        Piece rook = board[startRow][0];
                        if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                            // Check that the spaces between king and rook are empty
                            if (board[startRow][1] == null && board[startRow][2] == null && board[startRow][3] == null) {
                                PlayerColor oppColor = (piece.getColor() == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
                                // Cannot castle through or into check
                                if (!isSquareAttacked(startRow, 2, oppColor) && !isSquareAttacked(startRow, 3, oppColor)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                return false;

            default:
                return false;
        }
    }

    /**
     * Determines if a specific square is fundamentally threatened by a piece of the attacking color.
     * Ignores whose turn it currently is and complex moves (castling, etc.).
     */
    private boolean isSquareAttacked(int targetRow, int targetCol, PlayerColor attackingColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece attacker = board[r][c];
                if (attacker != null && attacker.getColor() == attackingColor) {
                    if (canPieceAttack(attacker, r, c, targetRow, targetCol)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Basic collision and trajectory calculation to see if a piece threatens a square.
     */
    private boolean canPieceAttack(Piece piece, int startRow, int startCol, int endRow, int endCol) {
        int rowDiff = endRow - startRow;
        int colDiff = endCol - startCol;

        if (rowDiff == 0 && colDiff == 0) return false;

        switch (piece.getType()) {
            case PAWN:
                int dir = (piece.getColor() == PlayerColor.WHITE) ? -1 : 1;
                // Pawns only attack diagonally
                return Math.abs(colDiff) == 1 && rowDiff == dir;
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
     * Returns true if the King of the specified color is currently under attack.
     */
    public boolean isInCheck(PlayerColor color) {
        PlayerColor opponentColor = (color == PlayerColor.WHITE) ? PlayerColor.BLACK : PlayerColor.WHITE;
        
        // Find the king
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board[r][c];
                if (piece != null && piece.getColor() == color && piece.getType() == PieceType.KING) {
                    return isSquareAttacked(r, c, opponentColor);
                }
            }
        }
        return false;
    }

    /**
     * Temporarily applies a move to the board and checks if it results in the current player's King being in check.
     */
    private boolean wouldMoveLeaveKingInCheck(int startRow, int startCol, int endRow, int endCol) {
        Piece pieceToMove = board[startRow][startCol];
        Piece targetPiece = board[endRow][endCol];
        
        // Temporarily apply move
        board[endRow][endCol] = pieceToMove;
        board[startRow][startCol] = null;
        
        // Handle temporary en passant capture removal
        Piece capturedEnPassant = null;
        int epRow = startRow;
        int epCol = endCol;
        if (pieceToMove.getType() == PieceType.PAWN && Math.abs(startCol - endCol) == 1 && targetPiece == null) {
            capturedEnPassant = board[epRow][epCol];
            board[epRow][epCol] = null;
        }

        // Check if the current player's king is in check after the simulation
        boolean inCheck = isInCheck(pieceToMove.getColor());

        // Revert move
        board[startRow][startCol] = pieceToMove;
        board[endRow][endCol] = targetPiece;
        if (capturedEnPassant != null) {
            board[epRow][epCol] = capturedEnPassant;
        }

        return inCheck;
    }

    /**
     * Scans the board to determine if the given player has ANY valid moves left.
     */
    private boolean hasAnyValidMoves(PlayerColor color) {
        for (int startRow = 0; startRow < 8; startRow++) {
            for (int startCol = 0; startCol < 8; startCol++) {
                Piece piece = board[startRow][startCol];
                // Try every move for every piece of that color
                if (piece != null && piece.getColor() == color) {
                    for (int endRow = 0; endRow < 8; endRow++) {
                        for (int endCol = 0; endCol < 8; endCol++) {
                            // If we find even one valid move, return true
                            if (isValidMove(startRow, startCol, endRow, endCol)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Executes the move if valid, handles captures, and switches turn.
     * Returns true if the move was successfully made.
     */
    public boolean movePiece(int startRow, int startCol, int endRow, int endCol) {
        return movePiece(startRow, startCol, endRow, endCol, null);
    }

    public boolean movePiece(int startRow, int startCol, int endRow, int endCol, PieceType promotion) {
        if (!isValidMove(startRow, startCol, endRow, endCol)) {
            return false;
        }

        Piece piece = board[startRow][startCol];
        
        // Build notation string
        String moveStr = getPieceSymbolForNotation(piece.getType()) + " " + 
                         (char)('a' + startCol) + (8 - startRow) + "-" + 
                         (char)('a' + endCol) + (8 - endRow);
                         
        if (piece.getType() == PieceType.KING && Math.abs(startCol - endCol) == 2) {
            moveStr = (endCol > startCol) ? "O-O" : "O-O-O";
        }
        if (promotion != null) {
            moveStr += "=" + getPieceSymbolForNotation(promotion);
        }
        
        moveHistory.add(moveStr);

        Piece targetPiece = board[endRow][endCol];
        
        // Handle normal captures
        if (targetPiece != null) {
            if (targetPiece.getColor() == PlayerColor.WHITE) {
                capturedWhitePieces.add(targetPiece);
            } else {
                capturedBlackPieces.add(targetPiece);
            }
        }
        
        // Handle En Passant capture removal
        if (piece.getType() == PieceType.PAWN && Math.abs(startCol - endCol) == 1 && board[endRow][endCol] == null) {
            Piece capturedPawn = board[startRow][endCol];
            if (capturedPawn != null) {
                if (capturedPawn.getColor() == PlayerColor.WHITE) {
                    capturedWhitePieces.add(capturedPawn);
                } else {
                    capturedBlackPieces.add(capturedPawn);
                }
            }
            board[startRow][endCol] = null; // Remove the captured pawn which is on the start row
        }

        // Handle Castling Rook move
        if (piece.getType() == PieceType.KING && Math.abs(startCol - endCol) == 2) {
            if (endCol > startCol) {
                // Kingside: move rook from 7 to 5
                Piece rook = board[startRow][7];
                board[startRow][5] = rook;
                board[startRow][7] = null;
                if (rook != null) rook.setHasMoved(true);
            } else {
                // Queenside: move rook from 0 to 3
                Piece rook = board[startRow][0];
                board[startRow][3] = rook;
                board[startRow][0] = null;
                if (rook != null) rook.setHasMoved(true);
            }
        }

        // Apply move
        board[endRow][endCol] = piece;
        board[startRow][startCol] = null;
        piece.setHasMoved(true);

        // Handle Promotion
        if (promotion != null && piece.getType() == PieceType.PAWN && (endRow == 0 || endRow == 7)) {
            board[endRow][endCol] = new Piece(promotion, piece.getColor());
            board[endRow][endCol].setHasMoved(true);
        }

        // Record this move for en passant validation next turn
        lastMove = new Move(startRow, startCol, endRow, endCol, piece);

        // Advance turn
        switchTurn();

        // Check for Game Over (Checkmate or Stalemate)
        if (!hasAnyValidMoves(currentTurn)) {
            setGameOver(true);
        }

        return true;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean isGameOver) {
        this.isGameOver = isGameOver;
    }

    public List<Piece> getCapturedWhitePieces() {
        return capturedWhitePieces;
    }

    public List<Piece> getCapturedBlackPieces() {
        return capturedBlackPieces;
    }

    public List<String> getMoveHistory() {
        return moveHistory;
    }

    private String getPieceSymbolForNotation(PieceType type) {
        switch (type) {
            case KING: return "\u265A";
            case QUEEN: return "\u265B";
            case ROOK: return "\u265C";
            case BISHOP: return "\u265D";
            case KNIGHT: return "\u265E";
            case PAWN: return "\u265F";
            default: return "";
        }
    }
}
