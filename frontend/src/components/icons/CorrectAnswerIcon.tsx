interface correcAnswerIconProps {
  className?: string
}
export default function CorrectAnswerIcon({ className }: correcAnswerIconProps) {
  return (
    <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect width="20" height="20" rx="10" fill="#47B881" />
      <path
        d="M13.6004 10.0007C13.6004 8.01243 11.9884 6.40039 10.0002 6.40039C8.01194 6.40039 6.3999 8.01243 6.3999 10.0007C6.3999 11.9889 8.01194 13.6009 10.0002 13.6009V14.6673C7.42283 14.6673 5.3335 12.578 5.3335 10.0007C5.3335 7.42332 7.42283 5.33398 10.0002 5.33398C12.5775 5.33398 14.6668 7.42332 14.6668 10.0007C14.6668 12.578 12.5775 14.6673 10.0002 14.6673V13.6009C11.9884 13.6009 13.6004 11.9889 13.6004 10.0007Z"
        fill="white"
      />
    </svg>
  )
}
