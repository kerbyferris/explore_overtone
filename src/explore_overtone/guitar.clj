;; A Guitar by Roger Allen.
(ns explore_overtone.guitar
  (:use [explore_overtone.stringed]
        [overtone.music pitch]      
        [overtone.studio inst]
        [overtone.sc envelope node server ugens]
        [overtone.sc.cgens mix]))

;; a map of chords to frets.
;; -1 indicates you mute that string
;; -2 indicates you leave that string alone
(def guitar-chord-frets
  {:A   [  0  0  2  2  2  0 ]
   :A7  [ -1 -1  2  2  2  3 ]
   :A9  [  0  0  2  4  2  3 ]
   :Am  [  0  0  2  2  1  0 ]

   :Bb  [ -1 -1  3  3  3  1 ]
   :Bb7 [ -1 -1  3  3  3  4 ]
   :Bb9 [ -1 -1  0  1  1  1 ]
   :Bbm [ -1 -1  3  3  2  1 ]

   :B   [ -1 -1  4  4  4  2 ]
   :B7  [ -1  2  1  2  0  2 ]
   :B9  [  2 -1  1  2  2  2 ]
   :Bm  [ -1 -1  4  4  3  2 ]

   :C   [ -1  3  2  0  1  0 ]
   :C7  [ -1  3  2  3  1 -1 ]
   :C9  [  3 -1  2  3  3  3 ]
   :Cm  [ -1 -1  5  5  4  3 ]

   :Db  [ -1 -1  3  1  2  1 ]
   :Db7 [ -1 -1  3  4  2  4 ]
   :Db9 [  4 -1  3  4  4  4 ]
   :Dbm [ -1 -1  2  1  2  0 ]

   :D   [ -1 -1  0  2  3  2 ]
   :D7  [ -1 -1  0  2  1  2 ]
   :D9  [ -1 -1  4  2  1  0 ]
   :Dm  [ -1  0  0  2  3  1 ]

   :Eb  [ -1 -1  5  3  4  3 ]
   :Eb7 [ -1 -1  1  3  2  3 ]
   :Eb9 [ -1 -1  1  0  2  1 ]
   :Ebm [ -1 -1  4  3  4  2 ]

   :E   [  0  2  2  1  0  0 ]
   :E7  [  0  2  0  1  0  0 ]
   :E9  [  0  2  0  1  3  2 ]
   :Em  [  0  2  2  0  0  0 ]

   :F   [ -1 -1  3  2  1  1 ]
   :F7  [ -1 -1  1  2  1  1 ]
   :F9  [ -1  3 -1  2  4  3 ]
   :Fm  [ -1 -1  3  1  1  1 ]

   :Gb  [ -1 -1  4  2  1  1 ]
   :Gb7 [ -1 -1  4  3  2  0 ]
   :Gb9 [ -1  4 -1  3  5  4 ]
   :Gbm [ -1 -1  4  2  2  2 ]

   :G   [  3  2  0  0  0  3 ]
   :G7  [  3  2  0  0  0  1 ]
   :G9  [ -1 -1  0  2  0  1 ]
   :Gm  [ -1 -1  5  3  3  3 ]

   :Ab  [ -1 -1  6  5  4  4 ]
   :Ab7 [ -1 -1  1  1  1  2 ]
   :Ab9 [ -1 -1  1  3  1  2 ]
   :Abm [ -1 -1  6  4  4  4 ]

   :Gadd5 [  3  2  0  0  3  3 ]
   :Cadd9 [ -1  3  2  0  3  3 ]
   :Dsus4 [ -1 -1  0  2  3  3 ]

   })

;; ======================================================================
;; an array of 6 guitar strings: EADGBE
(def guitar-string-notes (map note [:e2 :a2 :d3 :g3 :b3 :e4]))

;; ======================================================================
;; Main helper functions.  Use pick or strum to play the ukelele instrument.
(def pick (partial pick-string guitar-string-notes))
(def strum (partial strum-strings guitar-chord-frets guitar-string-notes))

;; ======================================================================
;; Create the guitar definst.  Now via the power of macros
(gen-stringed-inst guitar 6)
