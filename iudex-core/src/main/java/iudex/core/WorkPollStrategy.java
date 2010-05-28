package iudex.core;

public interface WorkPollStrategy
{
    boolean shouldReplaceQueue( VisitQueue visitQ );

    VisitQueue pollWork( VisitQueue current );
}
