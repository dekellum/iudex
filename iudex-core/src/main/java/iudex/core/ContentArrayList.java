package iudex.core;

import java.util.ArrayList;

public class ContentArrayList
    extends ArrayList<Content>
    implements ContentList
{
    public ContentArrayList()
    {
        super( 32 );
    }
}
