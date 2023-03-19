# Spelling Bee
A Spelling Bee Game for Android that is similar to that of New York Times' spelling bee game.

## ⚠️Noteworthy
This application is an adaptation of [Buzz](https://github.com/Plastix/Buzz) and most of the ui/design is from that
project.

## Reason

This application was inspired by two things: my love for challenging things and my love for playing word games.
The former reason was particularly sparked by [Jorge Castillo](https://twitter.com/JorgeCastilloPr/status/1629434465054253061) 
tweet, and I though what was the worst that could happen if I built an application using vanilla libraries: plain old Android sdk apis?
Your guess is as good as mine 😁

I have tried to keep code-base as simple and understandable as possible (simple is relative I know 🙂) but I figure the code-base 
is pretty understandable.

A couple of warnings before you decide to deep-dive into whatever I have written: the techniques used here use bare-metal
apis such as `Handlers`, `Loopers`,`Count-down Latch` among other stuff, but if you are like me, this wouldn't deter you from looking at the code-base :-)

Lastly, if you want to test the application out before looking at the code, please look at , download the .apk
and enjoy playing one of my favorite games.

## Architecture

1. The application's architecture is somewhat similar to MVVM, and inspired by  [CashApp's Molecule library](https://github.com/cashapp/molecule),
in that instead of using AAC ViewModel it uses presenter like composable functions to present state to ui-composables. The main reason for opting to use
composable functions instead of AAC ViewModel is because the presenter-like composable functions will respect the lifecycle of the ui-composables.
One common mis-conception I have seen is developers assuming composables' lifecycle respect/follow Activity's and other android-components (such as AAC ViewModek) lifecycles, and of cos they don't.
Notably, you can make your AAC ViewModel's respect your ui-composables lifecycle by scoping the former lifecycle to the latter but that would be against
the mantra of this project which is:_Keeping things as simple as possible and using the tools given to you without including 3rd party ones_

2. Handling of events was particularly tricky because the data-structures used to convey/broadcast events to the ui-composable require some special thought. In particular,
you can use `Channels` and `SharedFlow` to broadcast events but both have their cons and pros which need to be taken into consideration when designing your application.
In this application, a trade-off was made and `SharedFlow` was used as an event-bus to broadcast events from the presenter-like composables to
ui-composables. 

## Screenshots


## Running the Project

To run the project you only require to clone the project and build it using Android-Studio and your favorite build tool (gradle)

# License
All the code available under the MIT license.