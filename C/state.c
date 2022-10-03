#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "snake_utils.h"
#include "state.h"

/* Helper function definitions */
static void set_board_at(game_state_t* state, unsigned int x, unsigned int y, char ch);
static bool is_tail(char c);
static bool is_head(char c);
static bool is_snake(char c);
static char body_to_tail(char c);
static char head_to_body(char c);
static unsigned int get_next_x(unsigned int cur_x, char c);
static unsigned int get_next_y(unsigned int cur_y, char c);
static void find_head(game_state_t* state, unsigned int snum);
static char next_square(game_state_t* state, unsigned int snum);
static void update_tail(game_state_t* state, unsigned int snum);
static void update_head(game_state_t* state, unsigned int snum);


/* Task 1 */
game_state_t* create_default_state() {
  // TODO: Implement this function.
  
  game_state_t *gst = malloc(sizeof(game_state_t));
  char * default_board = "####################"
                          "#                  #"
"# d>D    *         #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"#                  #"
"####################";


  gst->num_rows = (unsigned int) 18;
  long unsigned int num_col = 20;
  long unsigned int i, j;
  gst -> board = (char**) malloc(sizeof(char*)*gst->num_rows);

  for (i=0; i<gst->num_rows;i++) {
  	gst -> board[i] = (char*) calloc(num_col, sizeof(char));
    // this_col = strncpy(this_col, i*gst->num_rows+default_board, gst->num_rows);  
  }

  for (j = 0; j < gst->num_rows * num_col; j++) {
    gst -> board[j/num_col][j%num_col] = default_board[j];
  }

  gst->num_snakes = 1;

  snake_t cute;
  cute.tail_x = 2;
  cute.tail_y = 2;
  cute.head_x = 4;
  cute.head_y = 2;
  cute.live = true;
  gst->snakes = (snake_t*) malloc(sizeof(snake_t)); 
  gst->snakes[0] = cute;

  // snake_t* cute = (snake_t*) malloc(sizeof(snake_t));
  // cute->tail_x = 2;
  // cute->tail_y = 2;
  // cute->head_x = 4;
  // cute->head_y = 2;
  // cute->live = true;
  // gst->snakes = (snake_t*) malloc(sizeof(snake_t)); 
  // gst->snakes[0] = *cute;
  // free(cute);

  return gst;
}


/* Task 2 */
void free_state(game_state_t* state) {
  // TODO: Implement this function.
  // int i, j;
  int i;
  for (i = 0; i < state->num_rows; i++) {
    free(state -> board[i]);
  }
  free(state->board);
  // for (j = 0; j < state->num_snakes; j++) {
  //   free(& state -> snakes[j]);
  // }
  free(state->snakes);
  free(state);
  return;
}

/* Task 3 */
void print_board(game_state_t* state, FILE* fp) {
  // TODO: Implement this function.
  int i;
  for (i = 0; i < state->num_rows; i++) {
    fprintf(fp, "%s\n", state->board[i]);
  }
  return;
}

/*
  Saves the current state into filename. Does not modify the state object.
  (already implemented for you).
*/
void save_board(game_state_t* state, char* filename) {
  FILE* f = fopen(filename, "w");
  print_board(state, f);
  fclose(f);
}


/* Task 4.1 */

/*
  Helper function to get a character from the board
  (already implemented for you).
*/
char get_board_at(game_state_t* state, unsigned int x, unsigned int y) {
  return state->board[y][x];
}

/*
  Helper function to set a character on the board
  (already implemented for you).
*/
static void set_board_at(game_state_t* state, unsigned int x, unsigned int y, char ch) {
  state->board[y][x] = ch;
}

/*
  Returns true if c is part of the snake's tail.
  The snake consists of these characters: "wasd"
  Returns false otherwise.
*/
static bool is_tail(char c) {
  // TODO: Implement this function.
  return c == 'a' || c == 'd' || c== 'w' || c == 's';
}

static bool is_body(char c) {
  return  c == '^' || c == '<' || c== '>' || c == 'v';
}

/*
  Returns true if c is part of the snake's head.
  The snake consists of these characters: "WASDx"
  Returns false otherwise.
*/
static bool is_head(char c) {
  // TODO: Implement this function.
  return c == 'A' || c == 'D' || c== 'W' || c == 'S' || c == 'x';
}

/*
  Returns true if c is part of the snake.
  The snake consists of these characters: "wasd^<>vWASDx"
*/
static bool is_snake(char c) {
  // TODO: Implement this function.
  return is_body(c) || is_head(c) || is_tail(c);
}

/*
  Converts a character in the snake's body ("^<>v")
  to the matching character representing the snake's
  tail ("wasd").
*/
static char body_to_tail(char c) {
  // TODO: Implement this function.
  if (c == '^') {
    return 'w';
  }
  else if (c == 'v') {
    return 's';
  }
  else if (c == '<') {
    return 'a';
  }
  else {
    return 'd';
  }
}

/*
  Converts a character in the snake's head ("WASD")
  to the matching character representing the snake's
  body ("^<>v").
*/
static char head_to_body(char c) {
  // TODO: Implement this function.
  if (c == 'W') {
    return '^';
  }
  else if (c == 'S') {
    return 'v';
  }
  else if (c == 'A') {
    return '<';
  }
  else {
    return '>';
  }
}

/*
  Returns cur_x + 1 if c is '>' or 'd' or 'D'.
  Returns cur_x - 1 if c is '<' or 'a' or 'A'.
  Returns cur_x otherwise.
*/
static unsigned int get_next_x(unsigned int cur_x, char c) {
  // TODO: Implement this function.
  if (c == '>' || c == 'd' || c == 'D') {
    return cur_x + 1;
  }
  else if (c == '<' || c == 'a' || c == 'A') {
    return cur_x - 1;
  }
  else {
    return cur_x;
  }
}

/*
  Returns cur_y + 1 if c is '^' or 'w' or 'W'.
  Returns cur_y - 1 if c is 'v' or 's' or 'S'.
  Returns cur_y otherwise.
*/
static unsigned int get_next_y(unsigned int cur_y, char c) {
  // TODO: Implement this function.
  if (c == '^' || c == 'w' || c == 'W') {
    return cur_y - 1;
  }
  else if (c == 'v' || c == 's' || c == 'S') {
    return cur_y + 1;
  }
  else {
    return cur_y;
  }
}

/*
  Task 4.2

  Helper function for update_state. Return the character in the cell the snake is moving into.

  This function should not modify anything.
*/

static char next_square(game_state_t* state, unsigned int snum) {
  // TODO: Implement this function.
  snake_t cute = state -> snakes[snum];
  char h = get_board_at(state, cute.head_x, cute.head_y);

  return get_board_at(state,get_next_x(cute.head_x, h), get_next_y(cute.head_y,h) );
}

static char next_square_tail(game_state_t* state, unsigned int snum) {
  // TODO: Implement this function.
  snake_t cute = state -> snakes[snum];
  char h = get_board_at(state, cute.tail_x, cute.tail_y);

  return get_board_at(state,get_next_x(cute.tail_x, h), get_next_y(cute.tail_y,h));
}

/*
  Task 4.3

  Helper function for update_state. Update the head...

  ...on the board: add a character where the snake is moving

  ...in the snake struct: update the x and y coordinates of the head

  Note that this function ignores food, walls, and snake bodies when moving the head.
*/
static void update_head(game_state_t* state, unsigned int snum) {
  // TODO: Implement this function.
  snake_t cute = state -> snakes[snum];
  char h = get_board_at(state, cute.head_x, cute.head_y);
  set_board_at(state, cute.head_x, cute.head_y, head_to_body(h));
  state ->snakes[snum].head_x = get_next_x(cute.head_x, h);
  state ->snakes[snum].head_y = get_next_y(cute.head_y, h);
  set_board_at(state, state ->snakes[snum].head_x, state ->snakes[snum].head_y, h);
  return;
}


/*
  Task 4.4

  Helper function for update_state. Update the tail...

  ...on the board: blank out the current tail, and change the new
  tail from a body character (^v<>) into a tail character (wasd)

  ...in the snake struct: update the x and y coordinates of the tail
*/
static void update_tail(game_state_t* state, unsigned int snum) {
  // TODO: Implement this function.
    snake_t cute = state -> snakes[snum];
    char body_next_to_tail = next_square_tail(state, snum);
    char h = get_board_at(state, cute.tail_x, cute.tail_y);
    set_board_at(state, cute.tail_x, cute.tail_y, ' ');
    state ->snakes[snum].tail_x = get_next_x(cute.tail_x, h);
    state ->snakes[snum].tail_y = get_next_y(cute.tail_y, h);
    set_board_at(state, state ->snakes[snum].tail_x, 
                state ->snakes[snum].tail_y, body_to_tail(body_next_to_tail));
  return;
}


/* Task 4.5 */
void update_state(game_state_t* state, int (*add_food)(game_state_t* state)) {
  // TODO: Implement this function.
  unsigned int sidx;
  char dest;
  for (sidx = 0; sidx < state->num_snakes; sidx++ ) {
    dest = next_square(state, sidx);
    if (dest == '*') {
      update_head(state, sidx);
      (*add_food)(state);
    }
    else if (dest == ' ') {
      update_head(state, sidx);
      update_tail(state, sidx);
    } 
    else {
      state->snakes[sidx].live = false;
      set_board_at(state, state->snakes[sidx].head_x,
                          state->snakes[sidx].head_y, 'x');
    }
  }

  return;
}


/* Task 5 */
game_state_t* load_board(char* filename) {
  // TODO: Implement this function.
  
  FILE *file = fopen(filename, "r");
  
  // non-existent file check
  if (file == NULL) {
    return NULL;
  }
  
  size_t capacity = 40;
  char *buffer = (char *) calloc(capacity, sizeof(char));
  int c;
  // size of the string currently stored in the buffer
  size_t size = 0;
  unsigned int num_rows = 0;

  // get num of rows
  while((c=fgetc(file))!=EOF) {
    if (c == '\n') {
      num_rows ++;
    }
  }

  // initialize the state to return
  game_state_t *gst = malloc(sizeof(game_state_t));
  gst->board = (char**) malloc(num_rows*sizeof(char*));
  gst->num_rows = num_rows;
  
  // rewind file pointer
  rewind(file);
  // unsigned int num_resize = 0;
  unsigned int cur_row = 0;
  while ((c=fgetc(file))!=EOF) {
    if (size >= capacity) {
      // num_resize ++;
      capacity = capacity*2;
      // accounting for null terminator
      buffer = realloc(buffer, capacity*sizeof(char));
      
    }
    if (c!='\n') {
      buffer[size] = (char) c;
      size ++;
    }
    else {
      gst->board[cur_row] = (char*) calloc(size+1, sizeof(char));
      strncpy(gst->board[cur_row], buffer, size);
      gst->board[cur_row][size] = '\0';
      cur_row ++;
      memset(buffer, 0, size);
      size = 0;
    }

  }
  fclose(file);
  free(buffer);
  return gst;
}


/*
  Task 6.1

  Helper function for initialize_snakes.
  Given a snake struct with the tail coordinates filled in,
  trace through the board to find the head coordinates, and
  fill in the head coordinates in the struct.
*/
static void find_head(game_state_t* state, unsigned int snum) {
  // TODO: Implement this function.
  unsigned int x = state->snakes[snum].tail_x;
  unsigned int y = state->snakes[snum].tail_y;
  char h = get_board_at(state, x, y);
  while (!is_head(h)) {
    x = get_next_x(x, h);
    y = get_next_y(y, h);
    h = get_board_at(state, x, y);
  }
  state->snakes[snum].head_x = x;
  state->snakes[snum].head_y = y;
  return;
}


/* Task 6.2 */
game_state_t* initialize_snakes(game_state_t* state) {
  // TODO: Implement this function.
  state->snakes  = (snake_t *) malloc(sizeof(snake_t));
  unsigned int num_snakes = 0;
  unsigned int r, c;
  char ch;
  for (r=0; r<state->num_rows;r++) {
    while ((ch=state->board[r][c])!='\0') {
      
      if (is_tail(ch)) {
        num_snakes ++;
        state->snakes = realloc(state->snakes,num_snakes*sizeof(snake_t));
        state->snakes[num_snakes-1].tail_x = c;
        state->snakes[num_snakes-1].tail_y = r;
        find_head(state, num_snakes-1);
      }

      c++;
    }
    c = 0;
  }
  state->num_snakes = num_snakes;

  return state;
}
