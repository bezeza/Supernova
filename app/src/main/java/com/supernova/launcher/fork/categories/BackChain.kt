package com.supernova.launcher.fork.categories

// What one Back closes in the categories, from the top: a tile's menu, the arranging of the tiles, the choosing of
// large icons, the category's screen. Each step closes its own and says whether there was anything to close; the
// first that did takes the Back, and the ones under it are left as they are.
internal object BackChain {

    fun run(
        menu: () -> Boolean,
        arranging: () -> Boolean,
        picking: () -> Boolean,
        screen: () -> Boolean
    ): Boolean = menu() || arranging() || picking() || screen()
}
