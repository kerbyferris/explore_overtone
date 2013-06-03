(ns explore-overtone.song-algorithm
  (:require [overtone.live :as o]
            [leipzig.live :as ll]
            [leipzig.melody :as lm]
            [leipzig.scale :as ls]
            [leipzig.canon :as lc]
            [overtone.synth.stringed :as strings]))

(strings/gen-stringed-synth ektara 1 true)

(defn pick [distort amp {midi :pitch, start :time, length :duration}]
    (let [synth-id (o/at start
                     (ektara midi :distort distort :amp amp :gate 1))]
      (o/at (+ start length) (o/ctl synth-id :gate 0))))

(defmethod ll/play-note :melody [note]
  (pick 0.3 1.0 note))

;; Code inspired by these comments:
;; http://www.reddit.com/r/musictheory/comments/1fe8y9/adding_chords_to_melody/
;; http://www.reddit.com/r/musictheory/comments/1fe8y9/adding_chords_to_melody/ca9gqtp

;; I think you could either start with chord structure & create melody
;; Or vice-verso.  This will start with the melody.

;; The first thing to do is find a melody.

;; Some advice from http://www.wikihow.com/Compose-a-Melody and http://smu.edu/totw/melody.htm
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


;; types of transposition to consider:
;; * pitch transposition (up a third, etc.)
;; * time augmentation (double, halve, etc.)
;; * inversion (upside down melody)
;; * retrograde (reverse the melody)
;; * combinations of all the above

;; One composition technique:
;; compose for 20-30mins - be free & create without judgement or attachment
;; clear mind for 1/2 hour or hour
;; come back, listen & find the best ideas from those 20-30 mins
;; iterate with those

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

(defn make-durations
  [dist beats]
  (let [durs (drop 1 (map first (take-while #(<= (second %) beats)
                                            (reductions #(vector %2 (+ (second %1) %2))
                                                        [0 0]
                                                        (repeatedly #(wrand dist))))))
        delta (- beats (apply + durs))
        durs (concat (butlast durs) (list (+ (last durs) delta)))]
    durs))
;; (make-durations [0 1 3 1 0.5] 16)

(defn make-call-phrase
  "create a random phrase suitable for the call part of a call/response"
   [num-beats duration-distribution start-pitch]
  (let [durations  (make-durations duration-distribution (* 2 num-beats))
        durations  (map #(/ % 2) durations)
        pitches    (take (count durations) (iterate (fn [x] (+ x (- 1 (rand-int 3)))) start-pitch))]
    (lm/phrase durations pitches)))

(defn make-response-phrase ;; FIXME
  [num-beats duration-distribution start-pitch]
  (make-call-phrase num-beats duration-distribution start-pitch))

(defn alter-phrase ;; FIXME add more (mirror/crab/table)
  [x]
  (let [y (+ 1 (rand-int 5))]
    ((lc/interval y) x)))

;; interesting breakdown of a tune http://www.youtube.com/watch?v=I6fjqw0FAQQ
;;   main motif / answer / main / transposed answer = 8 bars
;;   second motif / transposed second motif = 4 bars
;;   2x length phrase to finish = 4 bars
;;   total = 16 bars
;; another way to break it down:
;;   abaBcCd  where abc are all 2 bars each and d is 4 bars
;; my own analysis would break it down slightly differently since I don't like
;; that "d" is 4 bars.  the first part of d is like b I think.
;; what follows after is something new. how about...
;;   ab aB cC B'd
;; you need 4 parts a,b,c,d and each are 2 bars.
;; B and B' are alterations of b.  C is an alteration of c
;; a and c are "calls" and don't need peaks
;; b and d are "responses" and need peaks
(defn make-song []
  (let [beats1    (/ 64 8)
        dist1     [0 8 2 4 2 4 1]
        melody-a  (make-call-phrase     beats1 dist1 (rand-int 7))
        melody-b  (make-response-phrase beats1 dist1 0)
        melody-c  (make-call-phrase     beats1 dist1 (rand-int 7))
        melody-d  (make-response-phrase beats1 dist1 0)
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
         (lm/where :part (lm/is :melody)))))

(def melody (make-song))
;; (print melody)
;; (reduce #(+ %1 (:duration %2)) 0 melody) ;; 64

;; Your first step is figuring out what key your melody is in.
(def song-key (comp ls/low ls/G ls/minor))

(defn play-melody [speed key melody]
  (->> melody
       (lm/where :time speed)
       (lm/where :duration speed)
       (lm/where :pitch key)
       ll/play))

;; (play-melody (lm/bpm 120) song-key (make-song))

;; After you do that, assign diatonic chord types to each note in the
;; scale.  [This is theory-speak for take 3 notes, starting at each
;; note in the scale and every-other-note after that. Ex.  Cmajor is
;; CDEFGAB, The first (I) diatonic chord is CEG.  The 2nd (ii) is DFA
;; see http://en.wikipedia.org/wiki/Triad_(music)]
;;
;; If it's a major key, then the chord types will go Major I, minor
;; ii, minor iii, Major IV, Major V, minor vi, diminished viio.  [So,
;; CEG is a Major Chord, so the roman numeral index is captitalized I,
;; the 2nd chord is minor, so it is lower case ii. etc.]
;;
;; If it's a minor key, then it goes minor i, diminished iio , Major
;; III, minor iv, minor v, Major VI, Major VII. (the minor scale is
;; debatable depending on which type you use).  [The easy example for
;; minor is ABCDEFG.  ACE is a minor chord, etc.  For some more info
;; see http://en.wikipedia.org/wiki/Diatonic_function]
;;
;; [Not mentioned is the idea of using extended chords (more than 3
;; notes).  This is something to also consider.  See
;; http://en.wikipedia.org/wiki/Extended_chord]

(def song-chords [:C :Dm :Em :F :G :Am :Bdim])

;; Now your next step is to figure out where you want the
;; chords. Whenever that is, take that note and play the chord that
;; corresponds to it (or you can play 2 chords or 4 chords below it if it
;; flows better, but generally the one that directly corresponds is your
;; go-to chord. it takes some experimenting).

;; What your aiming for is a chord progression that both fits with your
;; melody and makes harmonic sense, i.e. if you were to play the chords
;; by themselves, it sounds very natural. and yeah, that's pretty much
;; how it goes.
;; [What this means is that chord progressions are not completely random.  There are certain paths that are far more well travelled.
;; http://www.hooktheory.com/blog/i-analyzed-the-chords-of-1300-popular-songs-for-patterns-this-is-what-i-found/
;; http://www.hooktheory.com/trends
;; ]