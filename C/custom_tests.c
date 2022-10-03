#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "asserts.h"
// Necessary due to static functions in state.c
#include "state.c"

char* COLOR_GREEN = "";
char* COLOR_RESET = "";

/* Look at asserts.c for some helpful assert functions */

int greater_than_forty_two(int x) {
  return x > 42;
}

bool is_vowel(char c) {
  char* vowels = "aeiouAEIOU";
  for (int i = 0; i < strlen(vowels); i++) {
    if (c == vowels[i]) {
      return true;
    }
  }
  return false;
}

/*
  Example 1: Returns true if all test cases pass. False otherwise.
    The function greater_than_forty_two(int x) will return true if x > 42. False otherwise.
    Note: This test is NOT comprehensive
*/
bool test_greater_than_forty_two() {
  int testcase_1 = 42;
  bool output_1 = greater_than_forty_two(testcase_1);
  if (!assert_false("output_1", output_1)) {
    return false;
  }

  int testcase_2 = -42;
  bool output_2 = greater_than_forty_two(testcase_2);
  if (!assert_false("output_2", output_2)) {
    return false;
  }

  int testcase_3 = 4242;
  bool output_3 = greater_than_forty_two(testcase_3);
  if (!assert_true("output_3", output_3)) {
    return false;
  }

  return true;
}

/*
  Example 2: Returns true if all test cases pass. False otherwise.
    The function is_vowel(char c) will return true if c is a vowel (i.e. c is a,e,i,o,u)
    and returns false otherwise
    Note: This test is NOT comprehensive
*/
bool test_is_vowel() {
  char testcase_1 = 'a';
  bool output_1 = is_vowel(testcase_1);
  if (!assert_true("output_1", output_1)) {
    return false;
  }

  char testcase_2 = 'e';
  bool output_2 = is_vowel(testcase_2);
  if (!assert_true("output_2", output_2)) {
    return false;
  }

  char testcase_3 = 'i';
  bool output_3 = is_vowel(testcase_3);
  if (!assert_true("output_3", output_3)) {
    return false;
  }

  char testcase_4 = 'o';
  bool output_4 = is_vowel(testcase_4);
  if (!assert_true("output_4", output_4)) {
    return false;
  }

  char testcase_5 = 'u';
  bool output_5 = is_vowel(testcase_5);
  if (!assert_true("output_5", output_5)) {
    return false;
  }

  char testcase_6 = 'k';
  bool output_6 = is_vowel(testcase_6);
  if (!assert_false("output_6", output_6)) {
    return false;
  }

  return true;
}

/* Task 4.1 */

bool test_is_tail() {
  // TODO: Implement this function.
  char testcase1 = 'a';
  bool output_1 = is_tail(testcase1);
  if (!assert_true("output_1", output_1)) {
    return false;
  }

  char testcase2 = 'w';
  bool output_2 = is_tail(testcase2);
  if (!assert_true("output_2", output_2)) {
    return false;
  }

  char testcase3 = 's';
  bool output_3 = is_tail(testcase3);
  if (!assert_true("output_3", output_3)) {
    return false;
  }

  char testcase4 = 'd';
  bool output_4 = is_tail(testcase4);
  if (!assert_true("output_4", output_4)) {
    return false;
  }

  char testcase5 = 'b';
  bool output_5 = is_tail(testcase5);
  if (!assert_false("output_5", output_5)) {
    return false;
  }

  return true;
}

bool test_is_head() {
  // TODO: Implement this function.
  char testcase1 = 'W';
  bool output_1 = is_head(testcase1);
  if (!assert_true("output_1", output_1)) {
    return false;
  }

  char testcase2 = 'A';
  bool output_2 = is_head(testcase2);
  if (!assert_true("output_2", output_2)) {
    return false;
  }

  char testcase3 = 'S';
  bool output_3 = is_head(testcase3);
  if (!assert_true("output_3", output_3)) {
    return false;
  }

  char testcase4 = 'D';
  bool output_4 = is_head(testcase4);
  if (!assert_true("output_4", output_4)) {
    return false;
  }

  char testcase5 = 'x';
  bool output_5 = is_head(testcase5);
  if (!assert_true("output_5", output_5)) {
    return false;
  }

  char testcase6 = 'f';
  bool output_6 = is_head(testcase6);
  if (!assert_false("output_6", output_6)) {
    return false;
  }

  return true;
}

bool test_is_snake() {
  // TODO: Implement this function.
  char testcase1 = 'W';
  bool output_1 = is_snake(testcase1);
  if (!assert_true("output_1", output_1)) {
    return false;
  }

  char testcase2 = '^';
  bool output_2 = is_snake(testcase2);
  if (!assert_true("output_2", output_2)) {
    return false;
  }

  char testcase3 = 'v';
  bool output_3 = is_snake(testcase3);
  if (!assert_true("output_3", output_3)) {
    return false;
  }

  char testcase4 = 'w';
  bool output_4 = is_snake(testcase4);
  if (!assert_true("output_4", output_4)) {
    return false;
  }

  char testcase5 = 'x';
  bool output_5 = is_snake(testcase5);
  if (!assert_true("output_5", output_5)) {
    return false;
  }

  char testcase6 = 'f';
  bool output_6 = is_snake(testcase6);
  if (!assert_false("output_6", output_6)) {
    return false;
  }

  return true;
}

bool test_body_to_tail() {
  // TODO: Implement this function.
  char testcase1 = '^';
  char output_1 = body_to_tail(testcase1);
  if (output_1 != 'w') {
    printf("%d", 1);
    return false;
  }
  
  char testcase2 = 'v';
  char output_2 = body_to_tail(testcase2);
  if (output_2 != 's') {
    printf("%d", 2);
    return false;
  }

  char testcase3 = '<';
  char output_3 = body_to_tail(testcase3);
  if (output_3 != 'a') {
    printf("%d", 3);
    return false;
  }

  char testcase4 = '>';
  char output_4 = body_to_tail(testcase4);
  if (output_4 != 'd') {
    printf("%d", 4);
    return false;
  }

  // char testcase5 = 'x';
  // char output_5 = body_to_tail(testcase5);
  // if (output_5) {
  //   return false;
  // }

  // char testcase6 = 'a';
  // char output_6 = body_to_tail(testcase6);
  // if (output_6) {
  //   return false;
  // }
  
  return true;
}

bool test_head_to_body() {
  // TODO: Implement this function.
  char testcase1 = 'W';
  char output_1 = head_to_body(testcase1);
  if (output_1 != '^') {
    printf("%d", 1);
    return false;
  }
  
  char testcase2 = 'S';
  char output_2 = head_to_body(testcase2);
  if (output_2 != 'v') {
    printf("%d", 2);
    return false;
  }

  char testcase3 = 'A';
  char output_3 = head_to_body(testcase3);
  if (output_3 != '<') {
    printf("%d", 3);
    return false;
  }

  char testcase4 = 'D';
  char output_4 = head_to_body(testcase4);
  if (output_4 != '>') {
    printf("%d", 4);
    return false;
  }
  return true;
}

bool test_get_next_x() {
  // TODO: Implement this function.
  char testcase1 = '>';
  unsigned int cur_x1 = 2;
  unsigned int output_1 = get_next_x(cur_x1, testcase1);
  if (output_1 != 3) {
    printf("%d", 1);
    return false;
  }

  char testcase2 = 'd';
  unsigned int cur_x2 = 4;
  unsigned int output_2 = get_next_x(cur_x2, testcase2);
  if (output_2 != 5) {
    printf("%d", 2);
    return false;
  }
  char testcase3 = 'D';
  unsigned int cur_x3 = 5;
  unsigned int output_3 = get_next_x(cur_x3, testcase3);
  if (output_3 != 6) {
    printf("%d", 3);
    return false;
  }



  char testcase4 = '<';
  unsigned int cur_x4 = 2;
  unsigned int output_4 = get_next_x(cur_x4, testcase4);
  if (output_4 != 1) {
    printf("%d", 4);
    return false;
  }

  char testcase5 = 'a';
  unsigned int cur_x5 = 5;
  unsigned int output_5 = get_next_x(cur_x5, testcase5);
  if (output_5 != 4) {
    printf("%d", 5);
    return false;
  }

  char testcase6 = 'A';
  unsigned int cur_x6 = 6;
  unsigned int output_6 = get_next_x(cur_x6, testcase6);
  if (output_6 != 5) {
    printf("%d", 6);
    return false;
  }

  char testcase7 = 'W';
  unsigned int cur_x7 = 6;
  unsigned int output_7 = get_next_x(cur_x7, testcase7);
  if (output_7 != 6) {
    printf("%d", 7);
    return false;
  }

  char testcase8 = 's';
  unsigned int cur_x8 = 6;
  unsigned int output_8 = get_next_x(cur_x8, testcase8);
  if (output_8 != 6) {
    printf("%d", 8);
    return false;
  }
  return true;
}

bool test_get_next_y() {
  // TODO: Implement this function.
  char testcase1 = '^';
  unsigned int cur_x1 = 2;
  unsigned int output_1 = get_next_y(cur_x1, testcase1);
  if (output_1 != 1) {
    printf("%d", 1);
    return false;
  }

  char testcase2 = 'w';
  unsigned int cur_x2 = 4;
  unsigned int output_2 = get_next_y(cur_x2, testcase2);
  if (output_2 != 3) {
    printf("%d", 2);
    return false;
  }
  char testcase3 = 'W';
  unsigned int cur_x3 = 5;
  unsigned int output_3 = get_next_y(cur_x3, testcase3);
  if (output_3 != 4) {
    printf("%d", 3);
    return false;
  }



  char testcase4 = 'v';
  unsigned int cur_x4 = 2;
  unsigned int output_4 = get_next_y(cur_x4, testcase4);
  if (output_4 != 3) {
    printf("%d", 4);
    return false;
  }

  char testcase5 = 's';
  unsigned int cur_x5 = 5;
  unsigned int output_5 = get_next_y(cur_x5, testcase5);
  if (output_5 != 6) {
    printf("%d", 5);
    return false;
  }

  char testcase6 = 'S';
  unsigned int cur_x6 = 6;
  unsigned int output_6 = get_next_y(cur_x6, testcase6);
  if (output_6 != 7) {
    printf("%d", 6);
    return false;
  }

  char testcase7 = 'a';
  unsigned int cur_x7 = 6;
  unsigned int output_7 = get_next_y(cur_x7, testcase7);
  if (output_7 != 6) {
    printf("%d", 7);
    return false;
  }

  char testcase8 = 'D';
  unsigned int cur_x8 = 6;
  unsigned int output_8 = get_next_y(cur_x8, testcase8);
  if (output_8 != 6) {
    printf("%d", 8);
    return false;
  }
  return true;

  
}

bool test_customs() {
  if (!test_greater_than_forty_two()) {
    printf("%s\n", "test_greater_than_forty_two failed.");
    return false;
  }
  if (!test_is_vowel()) {
    printf("%s\n", "test_is_vowel failed.");
    return false;
  }
  if (!test_is_tail()) {
    printf("%s\n", "test_is_tail failed");
    return false;
  }
  if (!test_is_head()) {
    printf("%s\n", "test_is_head failed");
    return false;
  }
  if (!test_is_snake()) {
    printf("%s\n", "test_is_snake failed");
    return false;
  }
  if (!test_body_to_tail()) {
    printf("%s\n", "test_body_to_tail failed");
    return false;
  }
  if (!test_head_to_body()) {
    printf("%s\n", "test_head_to_body failed");
    return false;
  }
  if (!test_get_next_x()) {
    printf("%s\n", "test_get_next_x failed");
    return false;
  }
  if (!test_get_next_y()) {
    printf("%s\n", "test_get_next_y failed");
    return false;
  }
  return true;
}

void init_colors() {
  if (!isatty(STDOUT_FILENO)) {
    return;
  }

  if (getenv("NO_COLOR") != NULL) {
    return;
  }

  char* term = getenv("TERM");
  if (term == NULL || strstr(term, "xterm") == NULL) {
    return;
  }

  COLOR_GREEN = "\033[0;32m";
  COLOR_RESET = "\033[0m";
}

bool test_and_print(char* label, bool (*run_test)()) {
  printf("\nTesting %s...\n", label);
  bool result = run_test();
  if (result) {
    printf("%sAll %s tests passed!%s\n", COLOR_GREEN, label, COLOR_RESET);
  } else {
    printf("Not all %s tests passed.\n", label);
  }
  return result;
}

int main(int argc, char* argv[]) {
  init_colors();

  if (!test_and_print("custom", test_customs)) {
    return 0;
  }

  return 0;
}
