#include "matrix.h"
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <omp.h>

// Include SSE intrinsics
#if defined(_MSC_VER)
#include <intrin.h>
#elif defined(__GNUC__) && (defined(__x86_64__) || defined(__i386__))
#include <immintrin.h>
#include <x86intrin.h>
#endif

/* Below are some intel intrinsics that might be useful
 * void _mm256_storeu_pd (double * mem_addr, __m256d a)
 * __m256d _mm256_set1_pd (double a)
 * __m256d _mm256_set_pd (double e3, double e2, double e1, double e0)
 * __m256d _mm256_loadu_pd (double const * mem_addr)
 * __m256d _mm256_add_pd (__m256d a, __m256d b)
 * __m256d _mm256_sub_pd (__m256d a, __m256d b)
 * __m256d _mm256_fmadd_pd (__m256d a, __m256d b, __m256d c)
 * __m256d _mm256_mul_pd (__m256d a, __m256d b)
 * __m256d _mm256_cmp_pd (__m256d a, __m256d b, const int imm8)
 * __m256d _mm256_and_pd (__m256d a, __m256d b)
 * __m256d _mm256_max_pd (__m256d a, __m256d b)
*/

/* Generates a random double between low and high */
double rand_double(double low, double high) {
    double range = (high - low);
    double div = RAND_MAX / range;
    return low + (rand() / div);
}

/* Generates a random matrix */
void rand_matrix(matrix *result, unsigned int seed, double low, double high) {
    srand(seed);
    for (int i = 0; i < result->rows; i++) {
        for (int j = 0; j < result->cols; j++) {
            set(result, i, j, rand_double(low, high));
        }
    }
}

/*
 * Returns the double value of the matrix at the given row and column.
 * You may assume `row` and `col` are valid. Note that the matrix is in row-major order.
 */
double get(matrix *mat, int row, int col) {
    // Task 1.1 TODO
    return mat->data[row*mat->cols + col];
}

/*
 * Sets the value at the given row and column to val. You may assume `row` and
 * `col` are valid. Note that the matrix is in row-major order.
 */
void set(matrix *mat, int row, int col, double val) {
    // Task 1.1 TODO
    mat->data[row*mat->cols + col] = val;
}

/*
 * Allocates space for a matrix struct pointed to by the double pointer mat with
 * `rows` rows and `cols` columns. You should also allocate memory for the data array
 * and initialize all entries to be zeros. `parent` should be set to NULL to indicate that
 * this matrix is not a slice. You should also set `ref_cnt` to 1.
 * You should return -1 if either `rows` or `cols` or both have invalid values. Return -2 if any
 * call to allocate memory in this function fails.
 * Return 0 upon success.
 */
int allocate_matrix(matrix **mat, int rows, int cols) {
    // Task 1.2 TODO
    // HINTS: Follow these steps.
    // 1. Check if the dimensions are valid. Return -1 if either dimension is not positive.
    // 2. Allocate space for the new matrix struct. Return -2 if allocating memory failed.
    // 3. Allocate space for the matrix data, initializing all entries to be 0. Return -2 if allocating memory failed.
    // 4. Set the number of rows and columns in the matrix struct according to the arguments provided.
    // 5. Set the `parent` field to NULL, since this matrix was not created from a slice.
    // 6. Set the `ref_cnt` field to 1.
    // 7. Store the address of the allocated matrix struct at the location `mat` is pointing at.
    // 8. Return 0 upon success.
    if (rows <= 0 || cols <= 0) {
        return -1;
    }
    matrix *res = (matrix *) calloc(1, sizeof(matrix));
    if (res == NULL)  {
        return -2;
    }
    res->data = (double *) calloc((long unsigned int) rows* (long unsigned int) cols, sizeof(double));
    if (res->data == NULL) {
        return -2;
    }
    res->rows = rows;
    res->cols = cols;
    res->parent = NULL;
    res->ref_cnt = 1;
    *mat = res;
    return 0;
}

/*
 * You need to make sure that you only free `mat->data` if `mat` is not a slice and has no existing slices,
 * or that you free `mat->parent->data` if `mat` is the last existing slice of its parent matrix and its parent
 * matrix has no other references (including itself).
 */
void deallocate_matrix(matrix *mat) {
    // Task 1.3 TODO
    // HINTS: Follow these steps.
    // 1. If the matrix pointer `mat` is NULL, return.
    // 2. If `mat` has no parent: decrement its `ref_cnt` field by 1. If the `ref_cnt` field becomes 0, then free `mat` and its `data` field.
    // 3. Otherwise, recursively call `deallocate_matrix` on `mat`'s parent, then free `mat`.
    if (mat == NULL) {
        return;
    }
    if (mat->parent == NULL) {
        mat->ref_cnt--;
        if (mat->ref_cnt == 0) {
            free(mat->data);
            free(mat);
        }
    }
    else {
        deallocate_matrix(mat->parent);
        free(mat);
    }

}

/*
 * Allocates space for a matrix struct pointed to by `mat` with `rows` rows and `cols` columns.
 * Its data should point to the `offset`th entry of `from`'s data (you do not need to allocate memory)
 * for the data field. `parent` should be set to `from` to indicate this matrix is a slice of `from`
 * and the reference counter for `from` should be incremented. Lastly, do not forget to set the
 * matrix's row and column values as well.
 * You should return -1 if either `rows` or `cols` or both have invalid values. Return -2 if any
 * call to allocate memory in this function fails.
 * Return 0 upon success.
 * NOTE: Here we're allocating a matrix struct that refers to already allocated data, so
 * there is no need to allocate space for matrix data.
 */
int allocate_matrix_ref(matrix **mat, matrix *from, int offset, int rows, int cols) {
    // Task 1.4 TODO
    // HINTS: Follow these steps.
    // 1. Check if the dimensions are valid. Return -1 if either dimension is not positive.
    // 2. Allocate space for the new matrix struct. Return -2 if allocating memory failed.
    // 3. Set the `data` field of the new struct to be the `data` field of the `from` struct plus `offset`.
    // 4. Set the number of rows and columns in the new struct according to the arguments provided.
    // 5. Set the `parent` field of the new struct to the `from` struct pointer.
    // 6. Increment the `ref_cnt` field of the `from` struct by 1.
    // 7. Store the address of the allocated matrix struct at the location `mat` is pointing at.
    // 8. Return 0 upon success.
    if (rows <= 0 || cols <= 0) {
        return -1;
    }
    matrix *res = (matrix *) malloc(sizeof(matrix));
    if (res == NULL)  {
        return -2;
    }
    res->data = from->data + offset;
    if (res->data == NULL) {
        return -2;
    }
    res->rows = rows;
    res->cols = cols;
    res->parent = from;
    res->ref_cnt = 1;
    from->ref_cnt++;
    *mat = res;
    return 0;
}

/*
 * Sets all entries in mat to val. Note that the matrix is in row-major order.
 */
void fill_matrix(matrix *mat, double val) {
    int total_ent_b = mat->rows * mat->cols;
    // store 4 values at a time
    __m256d _val = _mm256_set1_pd(val);
    #pragma omp parallel for 
    for (int pos = 0; pos <= total_ent_b - 4; pos+=4) {
        _mm256_storeu_pd(mat->data+pos, _val);
    }
    // tail case
    int pos = total_ent_b - total_ent_b%4;

    while (pos < total_ent_b) {
        *(mat->data+pos) = val;
        pos += 1;
    }
    
    return;
}

/*
 * Store the result of taking the absolute value element-wise to `result`.
 * Return 0 upon success.
 * Note that the matrix is in row-major order.
 */
int abs_matrix(matrix *result, matrix *mat) {
    // Task 1.5 TODO
    // __m256d _val = _mm256_set1_pd(val);

    // int total_ent_b = mat->rows * mat->cols;
    // #pragma omp parallel for 
    // for (int pos = 0; pos <= total_ent_b-4; pos++) {
    //     result->data[pos] = fabs(mat->data[pos]);
    //     result->data[pos+1] = fabs(mat->data[pos+1]);
    //     result->data[pos+2] = fabs(mat->data[pos+2]);
    //     result->data[pos+3] = fabs(mat->data[pos+3]);
    // }
    // int pos = total_ent_b - total_ent_b%4;
    // while (pos < total_ent_b) {
    //     *(result->data+pos) = fabs(mat->data[pos]);
    //     pos += 1;
    // }

    // return 0;

    int total_ent_b = mat->rows * mat->cols;
    // mask with the signed bit of double off
    __m256d _val = (__m256d) _mm256_set1_epi64x(0x7FFFFFFFFFFFFFFF);
    #pragma omp parallel for 
    for (int pos = 0; pos <= total_ent_b - 12; pos+=12) {
        // mask & num to make the signed bit zero
        __m256d slice = _mm256_loadu_pd((double const *) mat->data+pos);
        __m256d out = _mm256_and_pd(slice, _val);
        _mm256_storeu_pd(result->data+pos, out);        

        slice = _mm256_loadu_pd((double const *) mat->data+pos+4);
        out = _mm256_and_pd(slice, _val);
        _mm256_storeu_pd(result->data+pos+4, out);     

        slice = _mm256_loadu_pd((double const *) mat->data+pos+8);
        out = _mm256_and_pd(slice, _val);
        _mm256_storeu_pd(result->data+pos+8, out);     

        // slice = _mm256_loadu_pd((double const *) mat->data+pos+12);
        // out = _mm256_and_pd(slice, _val);
        // _mm256_storeu_pd(result->data+pos+12, out);     

        // slice = _mm256_loadu_pd((double const *) mat->data+pos+16);
        // out = _mm256_and_pd(slice, _val);
        // _mm256_storeu_pd(result->data+pos+16, out);     

        // slice = _mm256_loadu_pd((double const *) mat->data+pos+20);
        // out = _mm256_and_pd(slice, _val);
        // _mm256_storeu_pd(result->data+pos+20, out);     

        // slice = _mm256_loadu_pd((double const *) mat->data+pos+24);
        // out = _mm256_and_pd(slice, _val);
        // _mm256_storeu_pd(result->data+pos+24, out);  
    }
    // tail case
    int pos = total_ent_b - total_ent_b%12;
    while (pos < total_ent_b) {
        *(result->data+pos) = fabs(mat->data[pos]);
        pos += 1;
    }
    
    return 0;
}

/*
 * (OPTIONAL)
 * Store the result of element-wise negating mat's entries to `result`.
 * Return 0 upon success.
 * Note that the matrix is in row-major order.
 */
int neg_matrix(matrix *result, matrix *mat) {
    // Task 1.5 TODO
    int total_ent = mat->rows * mat->cols;
    int pos = 0;
    while (pos < total_ent) {
        result->data[pos] = -mat->data[pos];
        pos++;
    }

    return 0;
}

/*
 * Store the result of adding mat1 and mat2 to `result`.
 * Return 0 upon success.
 * You may assume `mat1` and `mat2` have the same dimensions.
 * Note that the matrix is in row-major order.
 */
int add_matrix(matrix *result, matrix *mat1, matrix *mat2) {
    // Task 1.5 TODO
    // int total_ent = mat1->rows * mat1->cols;
    // int pos = 0;
    // while (pos < total_ent) {
    //     result->data[pos] = mat1->data[pos] + mat2->data[pos];
    //     pos++;
    // }

    // return 0;
    int total_ent_b = mat1->rows * mat1->cols;
    // double * mat1_data =  mat1->data;
    // double * mat2_data = mat2->data;
    // double * result_data = result->data;
    
    // mask with the signed bit of double off

    #pragma omp parallel for 
    for (int pos = 0; pos <= total_ent_b - 4; pos+=4) {
        result->data[pos] = mat1->data[pos] + mat2->data[pos];
        result->data[pos+1] = mat1->data[pos+1] + mat2->data[pos+1];
        result->data[pos+2] = mat1->data[pos+2] + mat2->data[pos+2];
        result->data[pos+3] = mat1->data[pos+3] + mat2->data[pos+3];
        // mask & num to make the signed bit zero
        // __m256d slice1 = _mm256_loadu_pd((double const *) mat1_data+pos);
        // __m256d slice2 = _mm256_loadu_pd((double const *) mat2_data+pos);        
        // __m256d out = _mm256_add_pd(slice1, slice2);
        // _mm256_storeu_pd(result_data+pos, out);
               
        // slice1 = _mm256_loadu_pd((double const *) mat1_data+pos+4);
        // slice2 = _mm256_loadu_pd((double const *) mat2_data+pos+4);        
        // out = _mm256_add_pd(slice1, slice2);
        // _mm256_storeu_pd(result_data+pos+4, out);        
        
        // slice1 = _mm256_loadu_pd((double const *) mat1_data+pos+8);
        // slice2 = _mm256_loadu_pd((double const *) mat2_data+pos+8);        
        // out = _mm256_add_pd(slice1, slice2);
        // _mm256_storeu_pd(result_data+pos+8, out);        
        
        // slice1 = _mm256_loadu_pd((double const *) mat1->data+pos+12);
        // slice2 = _mm256_loadu_pd((double const *) mat2->data+pos+12);        
        // out = _mm256_add_pd(slice1, slice2);
        // _mm256_storeu_pd(result->data+pos+12, out);     

        // slice1 = _mm256_loadu_pd((double const *) mat1->data+pos+16);
        // slice2 = _mm256_loadu_pd((double const *) mat2->data+pos+16);        
        // out = _mm256_add_pd(slice1, slice2);
        // _mm256_storeu_pd(result->data+pos+16, out);        
        
        // slice1 = _mm256_loadu_pd((double const *) mat1->data+pos+20);
        // slice2 = _mm256_loadu_pd((double const *) mat2->data+pos+20);        
        // out = _mm256_add_pd(slice1, slice2);
        // _mm256_storeu_pd(result->data+pos+20, out);        
        
        // slice1 = _mm256_loadu_pd((double const *) mat1->data+pos+24);
        // slice2 = _mm256_loadu_pd((double const *) mat2->data+pos+24);        
        // out = _mm256_add_pd(slice1, slice2);
        // _mm256_storeu_pd(result->data+pos+24, out);                
    }
    // tail case
    int pos = total_ent_b - total_ent_b%4;
    while (pos < total_ent_b) {
        result->data[pos] = mat1->data[pos] + mat2->data[pos];
        pos += 1;
    }
    
    return 0;
}

/*
 * (OPTIONAL)
 * Store the result of subtracting mat2 from mat1 to `result`.
 * Return 0 upon success.
 * You may assume `mat1` and `mat2` have the same dimensions.
 * Note that the matrix is in row-major order.
 */
int sub_matrix(matrix *result, matrix *mat1, matrix *mat2) {
    // Task 1.5 TODO
    int total_ent = mat1->rows * mat1->cols;
    int pos = 0;
    while (pos < total_ent) {
        result->data[pos] = mat1->data[pos] - mat2->data[pos];
        pos++;
    }

    return 0;
}

void transpose_naive(int rows, int cols, double *dst, double *src) {
    #pragma omp parallel for collapse(2)
    for (int x = 0; x < rows; x++) {
        for (int y = 0; y < cols; y++) {
            dst[x + y * rows] = src[y + x * cols];
        }
    }

}



/*
 * Store the result of multiplying mat1 and mat2 to `result`.
 * Return 0 upon success.
 * Remember that matrix multiplication is not the same as multiplying individual elements.
 * You may assume `mat1`'s number of columns is equal to `mat2`'s number of rows.
 * Note that the matrix is in row-major order.
 */
int mul_matrix(matrix *result, matrix *mat1, matrix *mat2) {
    // Task 1.6 TODO
    // fill_matrix(result, 0);
    // // transposing the second matrix
    // double *transposed = (double *) malloc(mat2->rows*mat2->cols*sizeof(double));
    // transpose_naive(mat2->rows, mat2->cols, transposed, mat2->data);

    // for (int i = 0; i < mat1->rows; i++) {
    //     for (int j = 0; j < mat2->cols; j++) {
    //         for (int k = 0; k < mat1->cols; k++) {
    //             result->data[i*mat2->cols+j] += mat1->data[i*mat1->cols+k] * mat2->data[k*mat2->cols+j];
    //         }
    //     }
    // }

    // return 0;

    // transposing the second matrix
    
    
    // if (mat1->cols < 7 && mat1->rows < 7 && mat2->cols < 7) {
    //     for (int i = 0; i < mat1->rows; i++) {
    //         for (int j = 0; j < mat2->cols; j++) {
    //             result->data[i*mat2->cols+j] = 0;
    //             for (int k = 0; k < mat1->cols; k++) {
    //                 result->data[i*mat2->cols+j] += mat1->data[i*mat1->cols+k] * mat2->data[k*mat2->cols+j];
    //             }
    //         }
    //     }
    //     return 0;
    // }

    double *transposed = (double *) calloc(mat2->rows*mat2->cols, sizeof(double));
    transpose_naive(mat2->rows, mat2->cols, transposed, mat2->data);
    // int total_ent_b = mat1->rows * mat2->cols;
    #pragma omp parallel for collapse(2)
    for (int i = 0; i < mat1->rows; i++) {
        for (int j = 0; j < mat2->cols; j++) {
            double single_entry_result = 0;
            __m256d mulled = _mm256_set1_pd(0);
            for (int start1 = 0; start1 <= mat1->cols - 12; start1+=12) {
                // * not unrolled
                // __m256d slice1 = _mm256_loadu_pd((double const *) mat1->data+i*mat1->cols + start1);
                // __m256d slice2 = _mm256_loadu_pd((double const *) transposed+j*mat2->rows + start1); 
                // mulled = _mm256_fmadd_pd (slice1, slice2, mulled);
                

                // unrolled
                __m256d slice1 = _mm256_loadu_pd((double const *) mat1->data+i*mat1->cols + start1);
                __m256d slice2 = _mm256_loadu_pd((double const *) transposed+j*mat2->rows + start1); 
                mulled = _mm256_fmadd_pd (slice1, slice2, mulled);
                
                slice1 = _mm256_loadu_pd((double const *) mat1->data+i*mat1->cols + start1+4);
                slice2 = _mm256_loadu_pd((double const *) transposed+j*mat2->rows + start1+4); 
                mulled = _mm256_fmadd_pd (slice1, slice2, mulled);

                slice1 = _mm256_loadu_pd((double const *) mat1->data+i*mat1->cols + start1+8);
                slice2 = _mm256_loadu_pd((double const *) transposed+j*mat2->rows + start1+8); 
                mulled = _mm256_fmadd_pd (slice1, slice2, mulled);
                
            }
            double tmp_arr[4];
            _mm256_storeu_pd((double *) tmp_arr, mulled);
        
            single_entry_result = tmp_arr[0]+ tmp_arr[1]+ tmp_arr[2]+ tmp_arr[3];

            int start1 = mat1->cols - mat1->cols%12;
            

            while (start1 < mat1->cols) {
                // printf("pos: %d %d \n", i*mat1->cols+start1, j*mat2->rows+start1);
                // printf("multiplied: %f %f \n", *(mat1->data+i*mat1->cols + start1), *(transposed+j*mat2->rows + start1));
                single_entry_result += *(mat1->data+i*mat1->cols + start1)*(*(transposed+j*mat2->rows + start1));
                start1++;
            }
            // printf("\n");
            *(result->data + i*mat2->cols + j) = single_entry_result;
            
         }
     }

    //  for (int i = 0; i < mat1->rows*mat1->cols;i++) {
    //     printf("%f ", mat1->data[i]);
    //  }
    // printf("\n");
    // for (int i = 0; i < mat2->rows*mat2->cols;i++) {
    //     printf("%f ", mat2->data[i]);
    //  }
    // printf("\n");
    // for (int i = 0; i < mat2->rows*mat2->cols;i++) {
    //     printf("%f ", transposed[i]);
    //  }
    

    free(transposed);
    return 0;
}

/*
 * Store the result of raising mat to the (pow)th power to `result`.
 * Return 0 upon success.
 * Remember that pow is defined with matrix multiplication, not element-wise multiplication.
 * You may assume `mat` is a square matrix and `pow` is a non-negative integer.
 * Note that the matrix is in row-major order.
 */
int pow_matrix(matrix *result, matrix *mat, int pow) {
    // Task 1.6 TODO
    if (pow == 0) {
        for (int i = 0; i < mat->rows; i++) {
                result->data[i*mat->cols + i] = 1;
        }
        return 0;
    }
    if (pow == 1) {
        for (int i = 0; i < mat->rows; i++) {
            for (int j = 0; j < mat->cols; j++) {
                result->data[i*mat->cols + j] = mat->data[i*mat->cols + j];
            }
        }
        return 0;
    }

    // init temp = mat for temporary storage
    matrix *temp = NULL;
    allocate_matrix(&temp, mat->rows, mat->cols);
    for (int i = 0; i < mat->rows; i++) {
            for (int j = 0; j < mat->cols; j++) {
                temp->data[i*mat->cols + j] = mat->data[i*mat->cols + j];
            }
        }

    // create an id matrix
    matrix *y = NULL;
    allocate_matrix(&y, mat->rows, mat->cols);
    for (int i = 0; i < mat->rows; i++) {
        y->data[i*mat->cols + i] = 1;
    }

    // create a copy of y
    matrix *y_copy = NULL;
    allocate_matrix(&y_copy, mat->rows, mat->cols);
    for (int i = 0; i < mat->rows; i++) {
        y_copy->data[i*mat->cols + i] = 1;
    }
    
    // init result to be mat
        for (int i = 0; i < mat->rows; i++) {
                result->data[i*mat->cols + i] = 1;
        }


    // while (cur_pow*2 < pow) {
    //     mul_matrix(result, temp, mat);
    //     for (int i = 0; i < mat->rows; i++) {
    //         for (int j = 0; j < mat->cols; j++) {
    //             temp->data[i*mat->cols + j] = result->data[i*mat->cols + j];
    //         }
    //     }
    //     cur_pow++;
    // }

    while (pow > 1) {
        if (pow % 2 == 0) {
            // set result to mat^2, and temp to mat^2
            mul_matrix(result, temp, temp);
            pow = pow / 2;
            for (int i = 0; i < mat->rows; i++) {
                for (int j = 0; j < mat->cols; j++) {
                    temp->data[i*mat->cols + j] = result->data[i*mat->cols + j];
                }
            }
        }
        else {
            mul_matrix(y, temp, y_copy);
            mul_matrix(result, temp, temp);
            for (int i = 0; i < mat->rows; i++) {
                for (int j = 0; j < mat->cols; j++) {
                    temp->data[i*mat->cols + j] = result->data[i*mat->cols + j];
                }
            }
            for (int i = 0; i < mat->rows; i++) {
                for (int j = 0; j < mat->cols; j++) {
                    y_copy->data[i*mat->cols + j] = y->data[i*mat->cols + j];
                }
            }
            pow = (pow-1)/2;

        }
    }
    mul_matrix(result, temp, y);

    deallocate_matrix(temp);
    deallocate_matrix(y);
    deallocate_matrix(y_copy);

    return 0;
}