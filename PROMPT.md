Prompt:
I am building chess in Java using Swing, split into three files: ChessModel.java, ChessView.java, and ChessController.java. ChessView should extend JPanel and be hosted in a JFrame. ChessController should have the main method and wire the three classes together. ChessModel must have no swing imports. For now, just create the three class shells with placeholder comments describing what each class will do. The program should compile and open a blank window.

Prompt:
Fill in ChessModel.java. The model should track each piece location, the board layout (8x8 grid), which player's turn it is (alternating between white and black, starting with white).

Prompt:
Also in ChessModel.java, add logic to each of the pieces, pawns can move one space forward if the space is available or capture diagonally one space if an opposing colors piece is there. Also if the pawn has not moved yet all the pawn to move either one or two tiles forward. If the pawn has moved then only one tile is allowed. Additionally, add en passant logic

Prompt:
Fill in ChessView.java. It should take a reference to the model and draw everything the players see: the 8x8 board with alternating light green and a darker green, both sets of pieces (white and black). Show a centered game-over message when the game ends. The view should only read from the model - it must never change the game state.

Prompt:
Fill in ChessController.java. Add mouse controls so the player can move an available piece with Left mouse button and add the ability to draw temporary arrows with right mouse button that are removed once a player makes a move.

Prompt:
In ChessModel.java. Implement the ability to castle. Make sure the player can only do this if there are no pieces between the king and the rook its trying to castle with as well as ensure the king and the rook that is a part of the castle have not moved before the castle.

Prompt:
In ChessModel.java. Update the game logic to detect when a king is in check. Only allow the player in check to make a move the prevents the check. If no move exists end the game due to checkmate.

Prompt:
In ChessModel.java. Implement promotion when a pawn reaches the other side of the board. Allow the player to pick between knight, bishop, rook, or queen.

Prompt: