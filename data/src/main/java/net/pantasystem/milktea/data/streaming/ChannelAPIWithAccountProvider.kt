package net.pantasystem.milktea.data.streaming

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.pantasystem.milktea.api_streaming.channel.ChannelAPI
import net.pantasystem.milktea.common.Logger
import net.pantasystem.milktea.model.account.Account
import javax.inject.Inject


class ChannelAPIWithAccountProvider @Inject constructor(
    private val socketWithAccountProvider: SocketWithAccountProvider,
    private val loggerFactory: Logger.Factory
) {

    private val accountWithChannelAPI = mutableMapOf<Long, ChannelAPI>()
    private val logger = loggerFactory.create("ChannelAPIWithAccountProvider")
    private val mutex = Mutex()

    suspend fun get(account: Account): ChannelAPI {
        mutex.withLock {
            logger.debug("ChannelAPIWithAccountProvider get accountId=${account.accountId} hash=${hashCode()}")
            var channelAPI = accountWithChannelAPI[account.accountId]
            if (channelAPI != null) {
                return channelAPI
            }
            channelAPI = ChannelAPI(socketWithAccountProvider.get(account), loggerFactory)
            require(accountWithChannelAPI.put(account.accountId, channelAPI) == null)
            return channelAPI
        }
    }
}