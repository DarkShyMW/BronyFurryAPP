package net.pantasystem.milktea.data.infrastructure.messaging

import net.pantasystem.milktea.common.Encryption
import net.pantasystem.milktea.common.throwIfHasError
import net.pantasystem.milktea.data.api.misskey.MisskeyAPIProvider
import net.pantasystem.milktea.data.gettters.MessageAdder
import net.pantasystem.milktea.data.infrastructure.toGroup
import net.pantasystem.milktea.data.infrastructure.toUser
import net.pantasystem.milktea.model.account.GetAccount
import net.pantasystem.milktea.model.group.GroupDataSource
import net.pantasystem.milktea.model.messaging.MessageRelation
import net.pantasystem.milktea.model.messaging.MessagingRepository
import net.pantasystem.milktea.model.messaging.RequestMessageHistory
import net.pantasystem.milktea.model.user.UserDataSource
import javax.inject.Inject

class MessagingRepositoryImpl @Inject constructor(
    private val getAccount: GetAccount,
    private val encryption: Encryption,
    private val misskeyAPIProvider: MisskeyAPIProvider,
    private val groupDataSource: GroupDataSource,
    private val userDataSource: UserDataSource,
    private val messageAdder: MessageAdder,
) : MessagingRepository {

    override suspend fun findMessageSummaries(
        accountId: Long,
        isGroup: Boolean
    ): Result<List<MessageRelation>> = runCatching {
        val account = getAccount.get(accountId)
        val request = RequestMessageHistory(
            i = account.getI(
                encryption
            ), group = isGroup, limit = 100
        )

        val res = misskeyAPIProvider.get(account).getMessageHistory(request)
        res.throwIfHasError()
        res.body()!!.map {
            it.group?.let { groupDTO ->
                groupDataSource.add(groupDTO.toGroup(account.accountId))
            }
            it.recipient?.let { userDTO ->
                userDataSource.add(userDTO.toUser(account))
            }
            messageAdder.add(account, it)
        }
    }
}