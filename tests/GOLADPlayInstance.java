package tests;

import engine.Engine;
import javafx.util.Pair;
import rendering.renderers.MasterRenderer;
import states.GameState;
import states.GameStateManager;
import states.FadeTransState;
import tests.states.GOLADSavedGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by mjmcc on 2/13/2017.
 */
public class GOLADPlayInstance extends GameState
{
	private String gameId;

	private GOLADGame gameVars;

	private InputEvaluator inputEvaluator;
	private GameRenderer gameRenderer;

	private GameGui gameGui;

	private CellGrid grid;
	private GridData currentGridData;

	private List<Player> players;
	private int playerOn;

	private Turn currentTurn;

	public GOLADPlayInstance(int width, int height, GOLADGame gameVars)
	{
		this.gameVars = gameVars;

		this.grid = new CellGrid(width, height);
		this.gameGui = new GameGui();

		this.inputEvaluator = new InputEvaluator(Engine.getInputManager(), grid, gameGui);
		this.gameRenderer = new GameRenderer();

		this.players = new ArrayList<>();
		this.playerOn = -1;

		this.gameId = Integer.toString(hashCode());
	}

	public GOLADPlayInstance(GOLADGame gameVars, InputEvaluator inputEvaluator, GameRenderer gameRenderer, GOLADGameSerializer serializer,
							 GameGui gameGui, CellGrid grid, List<Player> players)
	{
		this.players = players;
		this.gameVars = gameVars;
		this.inputEvaluator = inputEvaluator;
		this.gameRenderer = gameRenderer;
		this.gameGui = gameGui;
		this.grid = grid;

		this.playerOn = -1;

		this.gameId = Integer.toString(hashCode());
	}

	@Override
	public void init()
	{
		currentTurn = new Turn(players.get(0), grid);
		currentGridData = grid.getGridData();
		goToNextPlayer();
	}

	@Override
	public void cleanUp()
	{

	}

	@Override
	public void tick(GameStateManager stateManager)
	{
		Player player = players.get(playerOn);
		player.constructTurn(currentTurn);
		visualizeTurn(currentTurn);

		//inputEvaluator.checkButtons();
		checkButtons();

		if (currentTurn.isOver())
		{
			submitTurn(currentTurn);

			runGridSim(currentGridData);
			currentGridData = grid.getGridData();

			checkPlayersEliminated(currentGridData.getPlayerOwnershipMap());

			goToNextPlayer();

			updateGui();

			currentTurn.reset(players.get(playerOn));

			if (checkEndState())
			{
				endGame(stateManager);
			}
		}
	}

	@Override
	public void render(MasterRenderer renderer)
	{

		gameRenderer.render(renderer, this);
	}

	@Override
	public void pause()
	{
		// save game
		String savePath = gameVars.SAVES_FOLDER_PTH + gameId + ".gpdb";
		GOLADSavedGame gameSave = gameVars.gameSerializer.writeToFile(this, gameVars, savePath);
		gameVars.addGameSave(gameSave);

		Engine.getGuiManager().hide(gameGui.getGuiScene());
	}

	@Override
	public void resume()
	{

		Engine.getGuiManager().show(gameGui.getGuiScene());
	}

	private void checkButtons()
	{
		if(gameGui.isMainMenuClicked())
			Engine.getStateManager().set(new FadeTransState(this, gameVars.MAIN_MENU, 0.5f));
	}

	private void updateGui()
	{
		Map<Player, List<Cell>> playerOwnershipMap = currentGridData.getPlayerOwnershipMap();

		for (Player player : players)
		{
			PlayerCard card = gameGui.getPlayerCard(player);
			if (!player.isEliminated())
			{
				List<Cell> playerCells = playerOwnershipMap.get(player);
				card.setCellCount(playerCells.size());
			} else
			{
				card.setCellCount(0);
			}
		}
	}

	/**
	 * Simulate life cycle among the entire grid, using the
	 * Conway game of life ruleset
	 */
	public Map<Cell, CellEnvironmentData> runGridSim(GridData gridData)
	{
		Map<Cell, CellEnvironmentData> cellEnvironmentMap = gridData.getCellEnvironmentMap();

		for (Cell cell : cellEnvironmentMap.keySet())
		{
			CellEnvironmentData enviroData = cellEnvironmentMap.get(cell);
			runConwayRuleset(cell, enviroData);
		}

		return cellEnvironmentMap;
	}

	/**
	 * Update the state of a specific cell, determined using the
	 * Conway game of life rules
	 */
	private void runConwayRuleset(Cell cell, CellEnvironmentData enviroData)
	{
		Player majPlayer = enviroData.getMajPlayer();
		int state = ConwayRules.resolveConwayRuling(cell, enviroData);

		// Death
		if (state == ConwayRules.DIED)
			grid.setCellState(cell, false, Player.NO_PLAYER, Cell.DEAD_COLOR);

		// Birth
		if (state == ConwayRules.BORN)
			grid.setCellState(cell, true, majPlayer, majPlayer.getColor());
	}

	/**
	 * Set the color of a cells dynamically as the
	 * player is creating their turn. This is used to help the player
	 * know where they are placing and removing cells.
	 */
	public void visualizeTurn(Turn t)
	{
		List<Pair<Integer, Integer>> additions = t.getCellAdditions();
		List<Pair<Integer, Integer>> removals = t.getCellRemovals();

		for (Pair<Integer, Integer> rem : removals)
		{
			Cell cell = grid.getCell(rem.getKey(), rem.getValue());
			cell.setVisualColor(Cell.DEAD_COLOR);
		}

		for (Pair<Integer, Integer> add : additions)
		{
			Cell cell = grid.getCell(add.getKey(), add.getValue());
			cell.setVisualColor(players.get(playerOn).getColor());
		}

	}

	/**
	 * Lock in/ Submit the current players turn.
	 * After a turn is finished, the decisions are used to set
	 * the grids cells.
	 */
	public void submitTurn(Turn turn)
	{
		Player player = players.get(playerOn);

		for (Pair<Integer, Integer> index : turn.getCellRemovals())
		{
			grid.setCellState(index.getKey(), index.getValue(), false, Player.NO_PLAYER, Cell.DEAD_COLOR);
			player.setBiomass(player.getBiomass() + 1);
		}

		for (Pair<Integer, Integer> index : turn.getCellAdditions())
		{
			grid.setCellState(index.getKey(), index.getValue(), true, player, player.getColor());
			player.setBiomass(player.getBiomass() - 1);
		}
	}

	/**
	 * Move to the next player in the queue
	 */
	public void goToNextPlayer()
	{
		do playerOn = (playerOn + 1) % players.size();
		while (players.get(playerOn).isEliminated());

		gameGui.setPlayerCardHighlight(players.get(playerOn));
	}

	/**
	 * Check if the game is over
	 */
	private boolean checkEndState()
	{
		int playersIn = 0;
		for (Player player : players)
			if (!player.isEliminated())
				playersIn++;

		return (playersIn == 1 || playersIn == 0);
	}

	/**
	 * Check if any players need to be eliminated.
	 * This method sets a flag in the player class to mark the player as
	 * eliminated. Then they can be skipped in the gotoNextPlayer method.
	 */
	private void checkPlayersEliminated(Map<Player, List<Cell>> playerOwnershipMap)
	{
		for (Player p : players)
			if (!playerOwnershipMap.containsKey(p))
				p.setEliminated(true);
	}

	/**
	 * End the game, decide a winner, show the end screen/ menu
	 */
	private void endGame(GameStateManager stateManager)
	{
		for (Player player : players)
			if (!player.isEliminated())
				gameVars.winningPlayer = player;

		stateManager.push(new FadeTransState(this, gameVars.END_GAME, gameVars.FADE_TIME));
	}

	public CellGrid getGrid()
	{
		return grid;
	}

	public GameGui getGui()
	{
		return gameGui;
	}

	public GridData getGridData()
	{
		return currentGridData;
	}

	public String getGameId()
	{
		return gameId;
	}

	public List<Player> getPlayers()
	{
		return players;
	}

	public void setGameId(String gameId)
	{
		this.gameId = gameId;
	}
}
