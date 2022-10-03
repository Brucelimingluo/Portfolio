#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include "snake_utils.h"
#include "state.h"

int main(int argc, char* argv[]) {
  char* in_filename = NULL;
  char* out_filename = NULL;
  game_state_t* state = NULL;

  // Parse arguments
  for (int i = 1; i < argc; i++) {
    if (strcmp(argv[i], "-i") == 0 && i < argc - 1) {
      in_filename = argv[i + 1];
      i++;
      continue;
    }
    if (strcmp(argv[i], "-o") == 0 && i < argc - 1) {
      out_filename = argv[i + 1];
      i++;
      continue;
    }
    fprintf(stderr, "Usage: %s [-i filename] [-o filename]\n", argv[0]);
    return 1;
  }

  // Do not modify anything above this line.

  /* Task 7 */

  // Read board from file, or create default board
  if (in_filename != NULL) {
    state = load_board(in_filename);
    if (state == NULL) {
      return -1;
    }
    state = initialize_snakes(state);
    // TODO: Load the board from in_filename
    // TODO: If the file doesn't exist, return -1
    // TODO: Then call initialize_snakes on the state you made
  } else {
    state = create_default_state();
    // TODO: Create default state
  }
  
  update_state(state, &deterministic_food);
  // TODO: Update state. Use the deterministic_food function
  // (already implemented in state_utils.h) to add food.

  // Write updated board to file or stdout
  if (out_filename != NULL) {
    save_board(state, out_filename);
    // TODO: Save the board to out_filename
  } else {
    FILE* out_file = fopen(out_filename, "r");
    int ch;
    while ((ch=fgetc(out_file))!=EOF) {
      printf("%c", ch);
    }
    fclose(out_file);
    // TODO: Print the board to stdout
  }

  free_state(state);
  // TODO: Free the state

  return 0;
}
