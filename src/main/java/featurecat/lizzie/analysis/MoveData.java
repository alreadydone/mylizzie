package featurecat.lizzie.analysis;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.Board;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds best move data
 */
public class MoveData {
    private final String coordinate;
    private final int playouts;
    private final double winrate;
    private final double probability;
    private final List<String> variation;

    public MoveData(String coordinate, int playouts, double winrate, double probability, List<String> variation) {
        this.coordinate = coordinate;
        this.playouts = playouts;
        this.winrate = winrate;
        this.probability = probability;
        this.variation = variation;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public int getPlayouts() {
        return playouts;
    }

    public double getWinrate() {
        return winrate;
    }

    public double getProbability() {
        return probability;
    }

    public List<String> getVariation() {
        return variation;
    }

    public String getMoveDisplayString() {
        if (Lizzie.optionSetting.isA1OnTop()) {
            return coordinate;
        } else {
            return flipAxisForMove(coordinate);
        }
    }

    public String getVariationDisplayString() {
        if (Lizzie.optionSetting.isA1OnTop()) {
            return String.join(" ", variation);
        } else {
            List<String> newVarAxis = variation.stream().map(MoveData::flipAxisForMove).collect(Collectors.toList());
            return String.join(" ", newVarAxis);
        }
    }

    private static String flipAxisForMove(String move) {
        int[] coords = Board.convertNameToCoordinates(move);
        int x = coords[0], y = coords[1];

        if (Board.isValid(x, y)) {
            return Board.alphabet.substring(x, x + 1) + (Board.BOARD_SIZE - y);
        } else {
            return "Pass";
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("coordinate", coordinate)
                .add("playouts", playouts)
                .add("winrate", winrate)
                .add("probability", probability)
                .add("variation", variation)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MoveData moveData = (MoveData) o;
        return playouts == moveData.playouts &&
                Double.compare(moveData.winrate, winrate) == 0 &&
                Double.compare(moveData.probability, probability) == 0 &&
                Objects.equal(coordinate, moveData.coordinate) &&
                Objects.equal(variation, moveData.variation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(coordinate, playouts, winrate, probability, variation);
    }
}
