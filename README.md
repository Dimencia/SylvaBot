# SylvaBot
A no-longer-functional twitch chatbot for SylvaCoin

I'm really shaky on the licensing and I know some of my modules weren't licensed with GPL, but from what I understand as long as I don't release any binaries then I don't have to adhere to any licenses
Someone let me know if I'm wrong, but I'm still only releasing this folder because I know it's all GPL and this is where almost all of my changes were made

## Features added ontop of CoeBot
New commands:
!playwithme - register for the list to play with sylva
!playwithme open/close/reset - Open, close, or reset registration
!playwithme list - Show a list of users registered (mod only)
!playwithme add (username), !playwithme remove (username) - Mod only
!newplayers (number of players) (optional: number to rotate) - Shows what players to use.  This will show you the players THEN move them, so it will match list, but use this when you want to know which ones can join so it rotates them afterward.

!bet start (max bet) (Yes or no question), !bet close, !bet cancel - Betting

!reset - Resets all counters as if the stream went offline and back up, DO NOT USE unless something isn’t working with announcements of follows/hosts/donations etc
!gooffline, !golive - In case the bot messes up detecting, but it should do this on its own now

!birthday (MM/DD) - Add your birthday and it will shout you out if you visit stream or discord that day
!birthday (user) (MM/DD) - Mod only

!discordstats - Tells you about discord, from twitch
!twitchstats - Tells you about twitch, from discord

!setstarttime - Sets the start time, default 9am.  So if sylva announces being late, the bot won’t count him as too late, because it will yell at him if he is.  After 1pm = 13, military time

!testfollow - Only works for the train

!moveto (channel) - Moves the bot to the other channel, but references all commands from sylva’s.  Counts coins in the other channel until it goes offline or you use !comeback, and all announcements go there.  Meant to be used for GMG or 2k when Sylva streams for them
!comeback - Pulls the bot out of any other channel it’s in.

!subscriber list/count - Lists or tells you how many subs there are.

!time or !sylvatime  - Tells you what time it is for Sylva right now

!pun - Gives a random pun
!ctof / !ftoc (number) - converts a temperature from C to F, or F to C
!trashtalk - Gives a random insult.  Usually really rude.
Themostinterestingbot, (message) - Cleverbot functionality.  May be broken, sometimes fixes itself
!give (username) (amount) - Any user, give your coins to someone else
!modgive (username) (amount) - Mods only, give someone coins without subtracting from yours
!modgiveall (amount) - Mods, gives coins to every viewer in the channel
!coins - Get your coins.  !coins (username) not yet implemented
!rank - Get your rank.  !rank (username) not yet implemented
!raiders (username1) (username2) (...) - Mods, adds each user as a ‘raider’ and gives them 300 coins (value adjustable, this is what ankhbot had for the number of coins)
!shutdown - Owner only, shuts down the bot and saves currency (used to shutdown cmenbot)
!nextrank - Gets your next possible rank
!say (message) - Owner only, it… says a thing.
!greenscreen - Owner only, opens sylva’s greenscreen.  In case he can’t be assed to click the taskbar button
!suggest (suggestion) - Any user, puts your suggestion in a file
!followage - You know what this does.  Issue with timezones right now but mostly working.
!giveaway start (cost) (sponsor) (time) (description) - Mods, Starts a giveaway.  Just do !giveaway ? if you want and it’ll tell you this.
!giveaway cancel - Mods, Fully cancels it
!giveaway reset - Mods, Clears and stops it - pretty much the same as cancel.
!giveaway count - Mods, Tells how many entries there are
!giveaway winner/stop/end/reroll - Mods, rolls a winner and ends entries.  Continue rerolling if necessary
!giveaway - Everyone, enters a giveaway if one is open.  They will be whispered that they have been entered
!quote - Gets a random quote.  Coebot required !quote random to do this.
!reset - Resets all counters, such as followers this stream, most recent follower, etc. Only use if it fails to detect sylva beginning stream

Sidenote: !quote add (Quote) will add that literal text.  Include your own tags for date and game at the end if you really want them, and usually best to put (!quote add “quote here” -Sylvacoin) because it won’t auto-add quotes around it or anything else.


Schedulelist commands - Goes through this list every 5 minutes and displays the next command if 
      Enough messages have passed.
!schedulelist add (commandName)
!schedulelist remove (commandName)
!schedulelist list
!schedulelist settimer (timeInMinutes) - Sets how often it reads from list, 5 mins default
!schedulelist setmessagedelay (numberOfMessages) - Sets how many messages must pass for it to read the command.

Death/other counters - Admin only - use the custom command as normal, with a +, -, or set (X) afterward
Note this works with any command, so you can create a savescums (or whatever) command and use it the same way, and it will put the count into its own separate text file for sylva to use onscreen.
Ex. !command add savescums Sylva has savescummed (_SAVESCUM_COUNT_) times this playthrough
!savescums +

!deaths +
!deaths set 0
!deaths -

New parsing parameters - use these when making or editing custom commands
(_NEWLINE_) - Splits the command into separate lines with a 1 sec delay between them.



Things removed from Coebot: 
(_COUNT_) parser - This does not function to count the number of times a command has been used, but instead is used for admins to increase/decrease like a death counter. You still have to use it if you want to create a counter-style command

!schedule (...) - Removed in favor of schedulelist

!raffle, !giveaway - Removed for a custom giveaway system

!quotes - Removed because it linked to a nonfunctioning coebot page, and listing all quotes will take too much screenspace.  Now tells you how many quotes there are.


