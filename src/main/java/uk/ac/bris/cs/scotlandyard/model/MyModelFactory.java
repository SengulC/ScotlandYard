package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.Set;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
		Model model = new Model() {

			Set<Observer> observers = new HashSet<>();
			MyGameStateFactory stateFactory = new MyGameStateFactory();
			Board.GameState currentGameState = stateFactory.build(setup, mrX, detectives);

			@Nonnull
			@Override
			public Board getCurrentBoard() {
				return currentGameState;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if (observer.equals(null)) {
					throw new NullPointerException("Observer to be registered can't be null.");
				} else {
					if (observers.contains(observer)) {
						throw new IllegalArgumentException("Observer already registered.");
					} else {
						observers.add(observer);
					}
				}
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if (observer.equals(null)) {
					throw new NullPointerException("Observer to be unregistered can't be null.");
				} else {
					if (!observers.contains(observer)) {
						throw new IllegalArgumentException("Observer isn't registered to be unregistered.");
					} else {
						observers.remove(observer);
					}
				}
			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return ImmutableSet.copyOf(observers);
			}

			@Override public void chooseMove(@Nonnull Move move) {
				currentGameState = currentGameState.advance(move);

				ImmutableSet<Piece> winner = currentGameState.getWinner();
				Observer.Event event;

				if (!winner.isEmpty()) {
					event = Observer.Event.GAME_OVER;
				} else {
					event = Observer.Event.MOVE_MADE;
				}

				for (Observer o : observers) {
					o.onModelChanged(getCurrentBoard(), event);
				}
			}
		};

		return model;
	}
}
