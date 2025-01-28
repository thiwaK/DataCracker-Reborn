package lk.thiwak.megarunii.game

import android.content.Context
import lk.thiwak.megarunii.log.Logger
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import kotlin.random.Random

class RaidShooter (
    val context: Context,
    gameConfig:JSONObject,
    serviceQueue: LinkedBlockingQueue<String>,
    threadQueue: LinkedBlockingQueue<String>
) :
    Game(context, gameConfig, serviceQueue, threadQueue) {

    private var previousGiftTimeMilli = System.currentTimeMillis()
    private var waitingTimeMilli: Long = 0

    companion object {

        // Waiting time range per shot
        const val MAX_WAITING_TIME = 5
        const val MIN_WAITING_TIME = 2

        // Zone values
        const val BULLS_EYE = 20
        const val RED_ZONE = 10
        const val BLACK_ZONE = 5
        const val OUTER_ZONE = 0

        // Possible shot
        const val MAX_SHOTS = 7
        const val MAX_BLACK_ZONE = 0
        const val MAX_RED_ZONE = 2
    }

    private fun calScoreCurrentRound(): Int {
        // random count of shots (black and red)
        val shotValues: MutableList<Int> = mutableListOf()
        for (i in 1..Random.nextInt(0, MAX_BLACK_ZONE)){
            shotValues.add(BLACK_ZONE)
        }
        for (i in 1..Random.nextInt(0, MAX_RED_ZONE)){
            shotValues.add(RED_ZONE)
        }

        // Rest is bull's eye
        val shotCountExcludeBullsEye = shotValues.size
        for (i in 1..(MAX_SHOTS - shotCountExcludeBullsEye)){
            shotValues.add(BULLS_EYE)
        }

        return shotValues.takeLast(MAX_SHOTS).sum()
    }

    fun calWaitingTime(): Long {
        // kotlin does end inclusive loop
        val waitingTime: MutableList<Int> = mutableListOf()
        for (i in 1..MAX_SHOTS){
            waitingTime.add(Random.nextInt(MIN_WAITING_TIME, MAX_WAITING_TIME))
        }

        val currentTimeMillis = System.currentTimeMillis()

        val dynamicWaitingTime = giftCount * 0.15
        waitingTimeMilli = (1000*waitingTime.sum()).toLong() + // Time taken for shots
                dynamicWaitingTime.toLong() +  // Dynamic delay for difficulty
                currentTimeMillis // Current time

        val currentTimeStr = getStrTime(currentTimeMillis)
        val giftReqTimeStr = getStrTime(waitingTimeMilli)
        Logger.info(context, "Waiting from $currentTimeStr to $giftReqTimeStr")

        return waitingTimeMilli
    }

    fun askForTheGift() {

        giftCount += 1
        Logger.info(context, "New gift request: $giftCount")

        if(System.currentTimeMillis() < previousGiftTimeMilli + waitingTimeMilli){
            Logger.error(context, "CRITICAL: WAITING TIME")
            Logger.warning(context, "Gift request aborted")
            return
        }


        // score
        val score = calScoreCurrentRound()
        gameScore += score
        Logger.info(context, "Score: Current($score) Total($gameScore)")

        // request body
        val requestBody = JSONObject(mapOf("score" to gameScore))

        // Time that last gift request was sent
        previousGiftTimeMilli = System.currentTimeMillis() // This will evaluate in the next round

        val gift = getGift(requestBody.toString())
        if(gift != 0 && gift != null){
            Logger.info(context, "You have won $gift Mb!")
        }else{
            Logger.info(context, "Maybe next time?")
        }

    }

}
