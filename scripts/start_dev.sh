SESSION='armchair'

cd /Users/maiwald/projects/armchair

# local
tmux new-session -d -s $SESSION -n shell

tmux split-window -p 50 -t $SESSION -h
tmux send-keys -t "$SESSION":shell "make watch" C-m

tmux split-window -p 50 -t $SESSION -v
tmux send-keys -t "$SESSION":shell "npm exec sass --watch src/sass:resources/public/compiled/css" C-m

tmux new-window -t $SESSION -n vim
tmux send-keys -t "$SESSION":vim "nvim" C-m

tmux select-window -t "$SESSION":shell
exec tmux attach -t $SESSION
