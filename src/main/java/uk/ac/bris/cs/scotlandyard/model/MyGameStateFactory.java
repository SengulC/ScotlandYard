package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ValueGraphBuilder;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	public class myTicketBoard implements Board.TicketBoard {

		private final ImmutableMap<ScotlandYard.Ticket, Integer> tickets;

		public myTicketBoard(@Nonnull ImmutableMap<ScotlandYard.Ticket, Integer> tickets) {
			this.tickets = tickets;
		}

		@Override
		public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
			return this.tickets.get(ticket);
		}
	}

	private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		Set<Move.SingleMove> singleMoves = new HashSet<>();
		for (int destination : setup.graph.adjacentNodes(source)) {
			Boolean flag = false;
			for (Player d : detectives) {
				if (d.location() == destination) {
					flag = true;
				}
			}

			if (flag) {
				continue;
			}

			for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
				if (player.hasAtLeast(t.requiredTicket(), 1) && !t.equals(ScotlandYard.Transport.FERRY)) {
					// check that t is NOT secret
					Move.SingleMove move = new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination);
					singleMoves.add(move);
				} if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1) && player.isMrX()) {
					Move.SingleMove secretMove = new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination);
					singleMoves.add(secretMove);
				}
			}
		}
		return singleMoves;
	}

	private static Boolean enoughTicketsForDM(Player player) {
		Integer count = 0;
		if (!player.isMrX()){
			return false;
		}
		if (player.hasAtLeast(ScotlandYard.Ticket.TAXI, 1)) {
			count++;
		} else if (player.hasAtLeast(ScotlandYard.Ticket.BUS, 1)) {
			count++;
		} else if (player.hasAtLeast(ScotlandYard.Ticket.UNDERGROUND, 1)) {
			count++;
		}

		if (count >= 2) {
			return true;
		} else {
			return false;
		}
	}

	private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
		// refactor to make it calling makesinglemoves on set of singlemoves
		Set<Move.DoubleMove> doubleMoves = new HashSet<>();
		Integer d1 = null;

		if (!player.isMrX() || !player.hasAtLeast(ScotlandYard.Ticket.DOUBLE, 1)) {
			doubleMoves.clear();
			return doubleMoves;
		}

		// loop1 through adj nodes to source
		for (int destination : setup.graph.adjacentNodes(source)) {
			Boolean flag1 = false;
			Boolean flag2 = false;

			// detective check
			for (Player d : detectives) {
				if (d.location() == destination) {
					flag1 = true;
				}
			}

			// flag1
			if (flag1) {
				continue;
			}

			// req t1 check
			for (ScotlandYard.Transport t1 : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()) ) {
				if (player.hasAtLeast(t1.requiredTicket(), 1)) {
					d1 = destination;
				} else if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1)) {
					d1 = destination;
				} else {
					continue;
				}

				// loop2 through adj nodes to d1
				for (int d2 : setup.graph.adjacentNodes(d1)) {
					flag2 = false;
					for (Player d : detectives) {
						if (d.location() == d2) {
							flag2 = true;
						}
					}

					if (flag2) {
						continue;
					}

					// req t2 check
					for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(d1, d2, ImmutableSet.of())) {
						if (t1.requiredTicket() == t2.requiredTicket()) {
							// same ticket for t1 and t2
							if (player.hasAtLeast(t2.requiredTicket(), 2)) {
								Move.DoubleMove move = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), d1, t2.requiredTicket(), d2);
								doubleMoves.add(move);
							}
						}

						// t1 and t2
						else if (player.hasAtLeast(t2.requiredTicket(), 1)) {
							Move.DoubleMove move = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), d1, t2.requiredTicket(), d2);
							doubleMoves.add(move);
						}

						// secret and t2
						if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1)) {
							Move.DoubleMove secretMove1 = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, d1,  t2.requiredTicket(), d2);
							doubleMoves.add(secretMove1);
						}
					}

					// t1 and secret
					if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1) && player.hasAtLeast(t1.requiredTicket(), 1)) {
						Move.DoubleMove secretMove2 = new Move.DoubleMove(player.piece(), source, t1.requiredTicket(), d1,  ScotlandYard.Ticket.SECRET, d2);
						doubleMoves.add(secretMove2);

					// 2 secret
					} if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 2)) {
						Move.DoubleMove secretMove = new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, d1,  ScotlandYard.Ticket.SECRET, d2);
						doubleMoves.add(secretMove);
					}
				} // adj nodes to d1 loop (d2)
			} // t1 loop
		} // adj nodes to source loop (destination)
		return doubleMoves;
	}

	private final class MyGameState implements Board.GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> availableMoves;
		private ImmutableSet<Piece> winner;

		MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {

			if (setup.graph.equals(ValueGraphBuilder.undirected()
					.<Integer, ImmutableSet<ScotlandYard.Transport>>immutable()
					.build())) {
				throw new IllegalArgumentException("Graph can't be empty!");
			}

			if (setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			this.setup = setup;

			this.remaining = remaining;

			if (mrX == null) throw new NullPointerException("MrX can't be null!");
			if (!mrX.isMrX()) throw new IllegalArgumentException("There's no MrX!");
			this.mrX = mrX;

			if (detectives.isEmpty()) throw new NullPointerException("There are no detectives!");
			this.detectives = detectives;
			Set<Player> detectivesWithNoDuplicates = new HashSet<>();
			for (Player p : detectives) {
				if (detectivesWithNoDuplicates.add(p) == false) {
					throw new java.lang.IllegalArgumentException("Duplicate detectives!");
				}
			}

			if (log == null) throw new IllegalArgumentException("Log entry is null!");
			this.log = log;

			// DETECTIVE X2 AND SECRET TICKET CHECK
			for (Player d : getDetectives()) {
				ImmutableMap<ScotlandYard.Ticket, Integer> tickets = d.tickets();
				for (Map.Entry<ScotlandYard.Ticket, Integer> entry : tickets.entrySet()) {
					ScotlandYard.Ticket ticket = entry.getKey();
					Integer tValue = entry.getValue();
					if (ticket.equals(ScotlandYard.Ticket.DOUBLE) && tValue > 0) {
						throw new IllegalArgumentException("Detective can't have x2 ticket!");
					}
					if (ticket.equals(ScotlandYard.Ticket.SECRET) && tValue > 0) {
						throw new IllegalArgumentException("Detective can't have secret ticket!");
					}
				}
			}

			// DOUBLE MRX CHECK
			// can't just use getPlayers() method because sets don't allow duplicates
			Set<Piece> players = new HashSet<>();
			players.add(mrX.piece());
			for (Player d : detectives) {
				if (!players.add(d.piece())) {
					throw new IllegalArgumentException("There's more than 1 MrX!");
				}
				players.add(d.piece());
			}

			// DETECTIVES LOCATION OVERLAP CHECK
			List<Integer> detectiveLocations = new ArrayList<>();
			for (Player d : detectives) {
				detectiveLocations.add(d.location());
			}
			Set<Integer> dLocationsNoDuplicates = new HashSet<>();
			for (Integer i : detectiveLocations) {
				if (dLocationsNoDuplicates.add(i) == false){
					throw new IllegalArgumentException("Duplicate location!");
				} else {
					dLocationsNoDuplicates.add(i);
				}
			}

			this.winner = getWinner();
			this.availableMoves = getAvailableMoves();
		}

		@Nonnull
		@Override
		public GameSetup getSetup() {
			return setup;
		}

		@Nonnull
		public List<Player> getDetectives() {
			return detectives;
		}

		@Nonnull
		public ImmutableSet<Piece> getPlayers() {
			// add MRX, then add detectives
			Set<Piece> players = new HashSet<>();
			players.add(mrX.piece());

			List<Player> detectives = getDetectives();
			for (Player d : detectives) {
				players.add(d.piece());
			}

			ImmutableSet<Piece> immutable = ImmutableSet.copyOf(players);
			return immutable;
		}

		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			Integer location;
			List<Player> detectives = getDetectives();
			for (Player d : detectives) {
				if (d.piece().equals(detective)) {
					return Optional.of(d.location());
				}
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if (piece.isMrX()) {
				ImmutableMap<ScotlandYard.Ticket, Integer> ticketsMrX = this.mrX.tickets();
				TicketBoard ticketBoardMrX = new myTicketBoard(ticketsMrX);
				return Optional.of(ticketBoardMrX);
			}
			List<Player> detectives = getDetectives();
			for (Player d : detectives) {
				if(d.piece().equals(piece)) {
					ImmutableMap<ScotlandYard.Ticket, Integer> piecesTickets = d.tickets();
					TicketBoard ticketBoard1 = new myTicketBoard(piecesTickets);
					return Optional.of(ticketBoard1);
				}
			}
			return Optional.empty();
		}

		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull
		public Boolean detectiveOnMrXsLocation() {
			for (Player d : detectives) {
				if (d.location() == mrX.location()) {
					return true;
				}
			}
			return false;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			Set<Piece> winners = new HashSet<>();
			// detectives win if...
			// A detective finishes a move on the same station as Mr X.
			// There are no unoccupied stations for Mr X to travel to.

			Set<Piece> detectivePieces = new HashSet<>();
			for (Player d : detectives) {
				detectivePieces.add(d.piece());
			}

			// if no detective can move
			if (!possibleMoves(detectives)) {
				winners.add(mrX.piece());
				return ImmutableSet.copyOf(winners);
			}

			if (detectiveOnMrXsLocation() || getAvailableMovesForAPlayer(mrX).isEmpty()) {
				winners.addAll(detectivePieces);
				return ImmutableSet.copyOf(winners);
			}


			// ISSUE IS: IF THERE'S NO WINNER YET. THERE SHOULD BE POSSIBLE MOVES.

			/*if (availableMoves.isEmpty()) {
				if (availableMoves.contains(mrX)) {
					winners.add(mrX.piece());
				} else {
					winners.addAll(detectivePieces);
				}
				return ImmutableSet.copyOf(winners);
			}*/

			// mrX wins if...
			// MrX manages to fill the log and the detectives subsequently fail to catch him with their final moves.
			// The detectives can no longer move any of their playing pieces.
			if (setup.moves.size() == log.size() && !detectiveOnMrXsLocation()) {
				winners.add(mrX.piece());
				return ImmutableSet.copyOf(winners);
			}

			winners.clear();
			return ImmutableSet.copyOf(winners);
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			Set<Move> moves = new HashSet<>();
			if (!possibleMoves(detectives) || detectiveOnMrXsLocation()) {
				return ImmutableSet.copyOf(moves);
			}

			// use for loop to iterate through the remaining player list, for each player, call the 2 methods
			for (Piece cPiece : remaining) {
				Player cPlayer = findAssociatedPlayerForPiece(cPiece);
				Set<Move.SingleMove> pieceSingleMoves = makeSingleMoves(setup, detectives, cPlayer, cPlayer.location());
				moves.addAll(pieceSingleMoves);
				if (setup.moves.size() - log.size() >= 2) {
					Set<Move.DoubleMove> pieceDoubleMoves = makeDoubleMoves(setup, detectives, cPlayer, cPlayer.location());
					moves.addAll(pieceDoubleMoves);
				}
			}

			return ImmutableSet.copyOf(moves);
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
			if(!availableMoves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

			Set<Piece> tempRemaining = new HashSet<>(remaining);
			List<LogEntry> tempLog = new ArrayList<>(log);
			List<Player> tempDetectives = new ArrayList<>(detectives);
			List<ScotlandYard.Ticket> moveTickets = move.accept(new Move.Visitor<>() {
				@Override
				public List<ScotlandYard.Ticket> visit(Move.SingleMove move) {
					List<ScotlandYard.Ticket> tickets = new ArrayList<>();
					tickets.add(move.ticket);
					return tickets;
				}

				@Override
				public List<ScotlandYard.Ticket> visit(Move.DoubleMove move) {
					List<ScotlandYard.Ticket> tickets = new ArrayList<>();
					tickets.add(move.ticket1); tickets.add(move.ticket2);
					return tickets;
				}
			});
			List<Integer> moveDestinations = move.accept(new Move.Visitor<>() {
				@Override
				public List<Integer> visit(Move.SingleMove move) {
					List<Integer> destinations = new ArrayList<>();
					destinations.add(move.destination);
					return destinations;
				}

				@Override
				public List<Integer> visit(Move.DoubleMove move) {
					List<Integer> destinations = new ArrayList<>();
					destinations.add(move.destination1);
					destinations.add(move.destination2);
					return destinations;
				}
			});
			Piece movePiece = move.accept(new Move.Visitor<Piece>() {
				@Override
				public Piece visit(Move.SingleMove move) {
					return move.commencedBy();
				}

				@Override
				public Piece visit(Move.DoubleMove move) {
					return move.commencedBy();
				}
			});

			// if all rounds have been played
			if (setup.moves.size() == tempLog.size() || detectiveOnMrXsLocation()) {
				tempRemaining.clear();
				return new MyGameState(
						setup,
						ImmutableSet.copyOf(tempRemaining),
						log,
						mrX,
						detectives
				);
			}

			Boolean reveal1 = false;
			if (setup.moves.size() > log.size()) {
				reveal1 = setup.moves.get(log.size());
			}
			Boolean reveal2 = false;
			if (setup.moves.size() > log.size()+1) {
				reveal2 = setup.moves.get((log.size())+1);
			}

			if (move.commencedBy() == mrX.piece()) {
				// MR X'S TURN---------------------------------------------------------------------------
				if (reveal1) { // reveal!
					tempLog.add(LogEntry.reveal(moveTickets.get(0), moveDestinations.get(0))); // add 1st move to log
				} else { // hide!
					tempLog.add(LogEntry.hidden(moveTickets.get(0))); // add 1st move to log
				}
				mrX = mrX.use(moveTickets.get(0)); // remove 1st used mrX ticket
				mrX = new Player(mrX.piece(), mrX.tickets(), moveDestinations.get(0)); // update mrX

				if (moveDestinations.size() == 2) { // if DOUBLE MOVE...
					if (reveal2) { // reveal!
						tempLog.add(LogEntry.reveal(moveTickets.get(1), moveDestinations.get(1)));
					} else { // hide!
						tempLog.add(LogEntry.hidden(moveTickets.get(1)));
					}
					mrX = mrX.use(moveTickets.get(1)); // remove 2nd used mrX ticket
					mrX = mrX.use(ScotlandYard.Ticket.DOUBLE);
					mrX = new Player(mrX.piece(), mrX.tickets(), moveDestinations.get(1)); // update mrX again
				}

				// change to detectives' turn...
				tempRemaining.clear(); // remove mrX from remaining

				for (Player detective : detectives) {
					tempRemaining.add(detective.piece()); // add all detectives to remaining
				}

				return new MyGameState(
						setup,
						ImmutableSet.copyOf(tempRemaining),
						ImmutableList.copyOf(tempLog),
						mrX,
						detectives
						);
			} else {
				// DETECTIVES' TURN---------------------------------------------------------------------------
				if (!remaining.contains(movePiece)) {
					// if the given player has already played their turn this round, return the current GameState
					return new MyGameState(
							setup,
							remaining,
							log,
							mrX,
							detectives
					);
				}
				Player detective = findAssociatedPlayerForPiece(movePiece);
				mrX = mrX.give(moveTickets.get(0));
				detective = detective.use(moveTickets.get(0));
				Player alteredDetective = new Player(movePiece, ImmutableMap.copyOf(detective.tickets()), moveDestinations.get(0)); // update detective
				for (Player d : detectives) {
					if (d.piece().equals(movePiece)) {
						tempDetectives.remove(d);
						tempDetectives.add(alteredDetective); // update tempDetectives list
					}
				}

				tempRemaining.remove(movePiece); // update tempRemaining list

				// change to MrX's turn if remaining is empty or none of the !remaining! detectives can move
				if (tempRemaining.isEmpty() || getAvailableMovesForASetOfPieces(tempRemaining).isEmpty()) {
					tempRemaining.clear();
					tempRemaining.add(mrX.piece());
				}

				return new MyGameState(
						setup,
						ImmutableSet.copyOf(tempRemaining),
						log,
						mrX,
						tempDetectives
				);
			}
		}

		Boolean possibleMoves(List<Player> players) {
			Set<Move> possibleMoves = new HashSet<>();
			for (Player p : players) {
				// iterate through players list and make SMs for each from their current location.
				possibleMoves.addAll(makeSingleMoves(setup, detectives, p, p.location()));
			}
			if (possibleMoves.isEmpty()) {
				return false;
			} else {
				return true;
			}
		}

		@Nonnull
		public ImmutableSet<Move> getAvailableMovesForAPlayer(Player cPlayer) {
			Set<Move> moves = new HashSet<>();
			moves.addAll(makeSingleMoves(setup, detectives, cPlayer, cPlayer.location()));
			if (setup.moves.size() - log.size() >= 2) {
				moves.addAll(makeDoubleMoves(setup, detectives, cPlayer, cPlayer.location()));
			}
			return ImmutableSet.copyOf(moves);
		}

		@Nonnull
		public ImmutableSet<Move> getAvailableMovesForASetOfPieces(Set<Piece> set) {
			Set<Move> moves = new HashSet<>();
			for (Piece p : set) {
				Player player = findAssociatedPlayerForPiece(p);
				moves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
				moves.addAll(makeDoubleMoves(setup, detectives, player, player.location()));
			}
			return ImmutableSet.copyOf(moves);
		}

		@Nonnull
		public Player findAssociatedPlayerForPiece(Piece piece) {
			List<Player> allPlayers = new ArrayList<>();
			allPlayers.addAll(getDetectives());
			allPlayers.add(mrX);

			Player returnPlayer = null;
			for (Player player : allPlayers) {
				if (player.piece().equals(piece)) {
					returnPlayer = player;
				}
			}

			return returnPlayer;
		}

	}
}
