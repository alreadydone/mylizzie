package featurecat.lizzie.rules;

public interface BoardStateChangeObserver {
    void mainStreamAppended(BoardHistoryNode newNodeBegin, BoardHistoryNode head);

    void mainStreamCut(BoardHistoryNode nodeBeforeCutPoint, BoardHistoryNode head);

    void headMoved(BoardHistoryNode oldHead, BoardHistoryNode newHead);

    void boardCleared(BoardHistoryNode initialNode, BoardHistoryNode initialHead);
}
