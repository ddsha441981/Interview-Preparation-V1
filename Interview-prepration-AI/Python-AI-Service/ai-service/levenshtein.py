import sys
import Levenshtein

def main():
    if len(sys.argv) != 3:
        print("Usage: python levenshtein.py <str1> <str2>")
        sys.exit(1)
    
    str1 = sys.argv[1]
    str2 = sys.argv[2]
    
    distance = Levenshtein.distance(str1, str2)
    
    print(f"Levenshtein Distance: {distance}")

if __name__ == '__main__':
    main()

