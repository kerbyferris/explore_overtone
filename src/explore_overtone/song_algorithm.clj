(ns explore-overtone.song-algorithm
  (:require [overtone.live           :as o]
            [overtone.synth.stringed :as strings]
            [oversampler.piano.inst  :as piano]
            [leipzig.live            :as ll]
            [leipzig.melody          :as lm]
            [leipzig.scale           :as ls]
            [leipzig.canon           :as lc]
            [leipzig.chord           :as lch]))

;; This code inspired by this question/comment on Reddit:
;;   http://www.reddit.com/r/musictheory/comments/1fe8y9/adding_chords_to_melody/
;;   http://www.reddit.com/r/musictheory/comments/1fe8y9/adding_chords_to_melody/ca9gqtp
;; that comment really broke down the algorithm to accomplish this simply

;; First setup some instruments to play with
;; guitar plays melody, piano plays accompaniment

;;(strings/gen-stringed-synth ektara 1 true)
(defn pick [distort pan {midi :pitch, start :time, length :duration, amp :dynamic}]
    (let [synth-id (o/at start
                     (ektara midi :distort distort :amp amp :gate 1 :pan pan
                             ;;:rvb-mix 0.35 :rvb-room 0.85 :rvb-damp 0.85
                             ;;:rvb-mix 0.0 :rvb-room 0.0 :rvb-damp 0.0
                             ;;:lp-freq 4000 :lp-rq 0.5
                             ))]
      (o/at (+ start length) (o/ctl synth-id :gate 0))))

(strings/gen-stringed-synth string3 3 true)
(defn pick3 [distort pan {pitches :pitch, start :time, length :duration, amp :dynamic}]
    (let [[n0 n1 n2] pitches
          synth-id   (o/at start
                           (string3 n0 n1 n2
                                    1  1  1
                                    :distort distort :amp amp))]
      (o/at (+ start length) (o/ctl synth-id :gate-0 0 :gate-1 0 :gate-2 0))))

(defn piano1 [{pitch :pitch, start :time, length :duration, amp :dynamic}]
    (let [synth-id  (o/at start (piano/sampled-piano :note pitch :level amp))]
      (o/at (+ start length) (o/ctl synth-id :gate 0))))

(defn piano3 [{pitches :pitch, start :time, length :duration, amp :dynamic}]
  (doall
   (doseq [n0 pitches]
     (let [synth-id0  (o/at start (piano/sampled-piano :note n0 :level amp))]
       (o/at (+ start length) (o/ctl synth-id0 :gate 0))))))

(defmethod ll/play-note :melody [note]
  ;;(piano1 note))
  (pick 0.2 -0.55 note))

(defmethod ll/play-note :accompaniment [notes]
  (piano3 notes))
  ;;(pick3 0.3 -0.55 notes))

;; I think you could either start with chord structure & create melody
;; Or vice-versa.
;; This code currently creates both independently since that is easiest.

;; Some googling found advice from
;; http://www.wikihow.com/Compose-a-Melody and
;; http://smu.edu/totw/melody.htm
;;
;; - mainly use runs along the scale
;; - use jumps sparingly, not back-to-back
;; - after a jump change direction back towards where you came from
;; - reach a "peak" in the 2nd half of the melodic sequence
;;   - "peak" can be a "trough"
;;   - peak can be at 50% all the way to 100% through.
;;   - I think you could call this area part the "response" to the "call"
;; - tones on the beat should be chordal tones
;;
;; - bass line can mirror melody contour for interest
;;
;; FIXME hardly any of the above is in the code below

;; this is the workhorse random function for this whole file
(defn wrand
  "given a vector of slice sizes, returns the index of a slice given a
  random spin of a roulette wheel with compartments proportional to
  slices."
  [slices]
  (let [total (reduce + slices)
        r (rand total)]
    (loop [i 0 sum 0]
      (if (< r (+ (slices i) sum))
        i
        (recur (inc i) (+ (slices i) sum))))))

;; I like dynamic phrases
(defn dynamic-phrase
  "Translates a sequence of durations, pitches and dynamics into a melody.
  e.g. (phrase [1 1 2] [7 6 4] [0.6 0.9 0.8])"
  [durations pitches dynamics]
  (->> (lm/phrase durations pitches) (lm/having :dynamic dynamics)))

(defn make-durations
  "given a dist vector and a number of beats to fill, return a vector of random durations that add up to beats"
  [dist beats]
  (let [durs (drop 1 (map first
                          (take-while #(<= (second %) beats)
                                      (reductions #(vector %2 (+ (second %1) %2))
                                                  [0 0]
                                                  (repeatedly #(wrand dist))))))
        delta (- beats (apply + durs))
        durs (concat (butlast durs) (list (+ (last durs) delta)))]
    durs))
;; (make-durations [0 1 3 1 0.5] 16)

(defn rand-melody-pitch-step
  "return a plausible random step for a melody"
  []
  ;; normally increment by +/-1, but try other variants with some frequency
  (let [pitch-step-base -8;-7 -6 -5 -4 -3 -2 -1  0  1  2  3  4  5  6  7  8
        pitch-step-dist [0  0  0  0  2  1  4 15  2 15  4  1  2  0  0  0  0]
        pitch-step (+ pitch-step-base (wrand pitch-step-dist))]
    pitch-step))

(defn make-call-phrase
  "create a random phrase suitable for the call part of a call/response"
  [num-beats duration-dist dyn-dist]
  (let [start-pitch (wrand [4 1 1 2 2 1 1 1])
        durations   (make-durations duration-dist (* 2 num-beats))
        durations   (map #(/ % 2) durations)
        pitches     (take (count durations)
                         (iterate (fn [x] (+ x (rand-melody-pitch-step))) start-pitch))
        dynamics    (take (count durations)
                          (repeatedly #(/ (+ (wrand dyn-dist) 3) 10)))]
    (dynamic-phrase durations pitches dynamics)))

;; FIXME make proper response. add "peak" in here somewhere
(defn make-response-phrase
  [num-beats duration-dist dyn-dist]
  (make-call-phrase num-beats duration-dist dyn-dist))

;; types of transposition to consider:
;; * pitch transposition (up a third, etc.)
;; * retrograde (reverse the melody)
;; * inversion (upside down melody)
;; o time augmentation (double, halve, etc.)
;; o combinations of all the above

(defn alter-phrase
  [the-phrase]
  (let [r (wrand [3 1 2])]
    (case r
      0 ((lc/interval (rand-melody-pitch-step)) the-phrase)
      1 (lc/crab the-phrase)
      2 ((lc/interval (rand-melody-pitch-step)) (lc/mirror the-phrase))
      )))
;; (def foo (make-call-phrase 8 [0 1 1]))
;; (alter-phrase foo)

;; Got the idea for adding an overall structure to the melody with this
;; interesting breakdown of a tune http://www.youtube.com/watch?v=I6fjqw0FAQQ
;;   main motif / answer / main / transposed answer = 8 bars
;;   second motif / transposed second motif = 4 bars
;;   2x length phrase to finish = 4 bars
;;   total = 16 bars
;; another way to break it down:
;;   abaBcCd  where abc are all 2 bars each and d is 4 bars
;; my own analysis would break it down slightly differently since I don't like
;; that "d" is 4 bars.  the first part of d is like b I think.
;; So, my own quick take on this is:
;;   ab aB cC B'd
;; you need 4 parts a,b,c,d and each are 2 bars.
;; B and B' are alterations of b.  C is an alteration of c
;; a and c are "calls" and don't need peaks
;; b and d are "responses" and need peaks
;;
;; FIXME this is just one type structure.  there are probably an infinite variety
;;
;; Another breakdown: http://www.mixedinkey.com/Book/Visualize-the-Structure-of-Dance-Music
;;
(defn make-melody
  [duration-dist dyn-dist num-beats melody-keyword]
  (let [num-phrase-beats (/ num-beats 8)
        melody-a  (make-call-phrase     num-phrase-beats duration-dist dyn-dist)
        melody-b  (make-response-phrase num-phrase-beats duration-dist dyn-dist)
        melody-c  (make-call-phrase     num-phrase-beats duration-dist dyn-dist)
        melody-d  (make-response-phrase num-phrase-beats duration-dist dyn-dist)
        melody-b1 (alter-phrase melody-b)
        melody-b2 (alter-phrase melody-b)
        melody-c1 (alter-phrase melody-c)]
    (->> melody-a
         (lm/then melody-b)
         (lm/then melody-a)
         (lm/then melody-b1)
         (lm/then melody-c)
         (lm/then melody-c1)
         (lm/then melody-b2)
         (lm/then melody-d)
         (lm/where :part (lm/is melody-keyword)))))

;; After you do that, assign diatonic chord types to each note in the
;; scale.  [This is theory-speak for take 3 notes, starting at each
;; note in the scale and every-other-note after that. Ex.  Cmajor is
;; CDEFGAB, The first (I) diatonic chord is CEG.  The 2nd (ii) is DFA
;; see http://en.wikipedia.org/wiki/Triad_(music)]
;; see http://en.wikipedia.org/wiki/Diatonic_function]
;;
;; [Not mentioned is the idea of using extended chords (more than 3
;; notes).  This is something to also consider.  See
;; http://en.wikipedia.org/wiki/Extended_chord]
;;
;; Now your next step is to figure out where you want the
;; chords. Whenever that is, take that note and play the chord that
;; corresponds to it (or you can play 2 chords or 4 chords below it if it
;; flows better, but generally the one that directly corresponds is your
;; go-to chord. it takes some experimenting).
;;
;; What your aiming for is a chord progression that both fits with your
;; melody and makes harmonic sense, i.e. if you were to play the chords
;; by themselves, it sounds very natural. and yeah, that's pretty much
;; how it goes.
;; [What this means is that chord progressions are not completely random.  There are certain paths that are far more well travelled.
;; http://www.hooktheory.com/blog/i-analyzed-the-chords-of-1300-popular-songs-for-patterns-this-is-what-i-found/
;; http://www.hooktheory.com/trends
;; ]
;;
;; FIXME should be different than make-melody
;;
(defn make-accompaniment
  [duration-dist dyn-dist num-beats accompaniment-keyword]
  (make-melody duration-dist dyn-dist num-beats accompaniment-keyword))

;; ======================================================================
;; http://tweakheadz.com/how-to-make-original-drum-tracks/
;;
;; Melody:
;; 1 make 1-bar pattern
;;   A
;; 2 copy/paste to make 2-bar pattern.  add subtle changes to 2nd bar
;;   AB
;; 3 CP to make 4-bar pattern.  add subtle changes to 4th bar
;;   ABAC
;; 4 CP 8-bar.  add subtle changes to 8th bar
;;   ABACABAD
;; 5 CP 16-bar.  add to 16th bar.  Make significant change to bar 16, because this is a major pillar if the song, you should design this fill to lead into the next part of the music.
;;   ABACABADABACABAE
;; Summary: A as base.  B,C,D are subtle changes.  E is dramatic change
;;
;; Chorus
;; * same basic pattern but make the hits louder.
;; * ex. hi hats to crash cymbals.
;; * as you approach bar 8 of the chorus, you want to drum up
;;   some excitement as the song should be peaking here.
;;
(comment
  (def snare (sample (freesound-path 26903)))
  (def kick (sample (freesound-path 2086)))
  (def close-hihat (sample (freesound-path 802)))
  (def open-hihat (sample (freesound-path 26657)))
  (def clap (sample (freesound-path 48310)))
  (def gshake (sample (freesound-path 113625)))

  (defmethod ll/play-note :drums0 [{p :pitch}]
    (case p
      0 nil
      1 (snare)
      2 (kick)
      3 (close-hihat)
      4 (open-hihat)
      5 (clap)
      6 (gshake)))

  (def beats0     ;; 1   2   3   4   5   6   7   8
    (->> (lm/phrase [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1]
                    [2 0 1 0 2 0 1 0 2 0 1 0 2 0 1 0]) ;; <- adjust
         (lm/with (lm/phrase
                   [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1]
                   [0 0 5 0 0 0 5 0 0 0 5 0 0 0 5 0]))
         (lm/with (lm/phrase
                   [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1]
                   [3 0 3 0 3 0 3 0 3 0 3 0 3 0 3 0]))
         (lm/with (lm/phrase
                   [1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1]
                   [6 6 0 6 6 6 0 6 6 6 0 6 6 6 0 6]))
         (lm/where :time (lm/bpm (* 2 124)))
         (lm/where :duration (lm/bpm (* 2 124)))))

  (ll/jam beats0)
)

;; ======================================================================
(def inc2 (comp inc inc))
(defn triad
  "translates to a chord with tonic at start-index"
  [key start-index]
  (take 3 (map key (iterate inc2 start-index))))

(defn seventh
  "translates to a chord with tonic at start-index"
  [key start-index]
  (take 4 (map key (iterate inc2 start-index))))

(defn ninth
  "translates to a chord with tonic at start-index"
  [key start-index]
  (take 5 (map key (iterate inc2 start-index))))

;; a song has a melody along with chords as accompaniment
;; at this point, the chords are just the tonic of the chord
(defn make-song []
  (let [num-beats 64]
    (->> (make-melody
          ;;0 0.5 1 1.5 2 2.5 3 3.5 4 durations
          [ 0  8  8  3  4  2  2  1  1]
          ;;.3 .4 .5 .6 .7 .8 .9 dynamics
          [  1  4  6  8  6  2  1]
          num-beats
          :melody)
         (lm/with (make-accompaniment
                   ;; 0 .5 1 1.5 2 2.5 3 3.5 4  longer durations than melody
                   [  0  0 1  2  4  1  3  1  3]
                   ;;.3 .4 .5 .6 .7 .8 .9 dynamics
                   [  1  2  4  6  8  6  2]
                   num-beats
                   :accompaniment)))))

(defn play-song [speed key song]
  (->> song
       (lm/where :time speed)
       (lm/where :duration speed)
       (lm/wherever (fn [x] (= :melody (:part x))) :pitch key)
       (lm/wherever (fn [x] (= :accompaniment (:part x))) :pitch (partial ninth key))  ;; FIXME triad/7th/9th select
       ll/play))

;; Figure out what key your melody is in, give it a tempo, a key

;; (o/recording-start "random_access_melodies.wav")
(do
  (def the-song (make-song))
  (def song-bpm (lm/bpm (+ 80 (rand-int 100))))
  (def song-key (comp ls/low
                   (rand-nth [ls/C ls/D ls/E ls/F ls/G ls/A])
                   (rand-nth [ls/major ls/minor ls/mixolydian ls/phrygian ls/blues])))
  ;; then play it
  (play-song song-bpm song-key the-song)
)
;; (o/recording-stop)
