package fi.dy.masa.itemscroller.data;

public class DataManager
{
    private static final DataManager INSTANCE = new DataManager();

    public DataManager()
    {
    }

    public static DataManager getInstance() { return INSTANCE; }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            // Do Something
        }
    }

    public void onGameInit()
    {
        // Add Servux handler
    }

    public void onWorldLoadPre()
    {
        // Add Servux handler
    }

    public void onWorldJoin()
    {
        // Add Servux handler
    }
}
