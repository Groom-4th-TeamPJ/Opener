interface correcAnswerIconProps {
  className?: string
}
export default function CorrectAnswerIcon({ className }: correcAnswerIconProps) {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
    >
      <rect width="18" height="18" rx="9" fill="#47B881" />
      <path
        d="M12.2402 9.00078C12.2402 7.21138 10.7894 5.76055 8.99999 5.76055C7.21059 5.76055 5.75975 7.21138 5.75975 9.00078C5.75975 10.7902 7.21059 12.241 8.99999 12.241V13.2008C6.68039 13.2008 4.79999 11.3204 4.79999 9.00078C4.79999 6.68119 6.68039 4.80078 8.99999 4.80078C11.3196 4.80078 13.2 6.68119 13.2 9.00078C13.2 11.3204 11.3196 13.2008 8.99999 13.2008V12.241C10.7894 12.241 12.2402 10.7902 12.2402 9.00078Z"
        fill="white"
      />
    </svg>
  )
}
